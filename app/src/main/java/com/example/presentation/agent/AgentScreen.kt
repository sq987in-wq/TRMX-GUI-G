package com.example.presentation.agent

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AgentMessage
import com.example.model.MessageRole
import com.example.model.ServerStatus
import com.example.presentation.components.GlassCard
import com.example.ui.theme.DeckAmber
import com.example.ui.theme.DeckBackground
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckEmerald
import com.example.ui.theme.DeckPurple
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextPrimary
import com.example.ui.theme.DeckTextSecondary
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalText

@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.session?.messages?.size) {
        val count = uiState.session?.messages?.size ?: 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    if (uiState.showApiKeyDialog) {
        ApiKeySettingsDialog(
            initialGeminiKey = uiState.geminiApiKey,
            initialOpenAiKey = uiState.openaiApiKey,
            onDismiss = { viewModel.setShowApiKeyDialog(false) },
            onSave = { gemini, openai -> viewModel.saveApiKeys(gemini, openai) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeckBackground)
    ) {
        // Clean Top Action Bar with Engine Dropdown & Key Settings
        var engineMenuExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Engine Selector Dropdown
            Box {
                val currentEngineName = when (uiState.selectedEngine) {
                    "GEMINI_CLOUD" -> "Gemini 2.5 Flash"
                    "OPENAI_CLOUD" -> "GPT-4o Mini"
                    else -> "Local CLI Assistant"
                }
                val currentEngineColor = when (uiState.selectedEngine) {
                    "GEMINI_CLOUD" -> DeckCyan
                    "OPENAI_CLOUD" -> DeckEmerald
                    else -> DeckPurple
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentEngineColor.copy(alpha = 0.15f))
                        .border(1.dp, currentEngineColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                        .clickable { engineMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("ai_engine_selector"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(currentEngineColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentEngineName,
                        color = DeckTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select AI Engine",
                        tint = currentEngineColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = engineMenuExpanded,
                    onDismissRequest = { engineMenuExpanded = false },
                    modifier = Modifier
                        .background(DeckSurfaceElevated)
                        .border(1.dp, DeckBorderGlass, RoundedCornerShape(8.dp))
                ) {
                    listOf(
                        Triple("GEMINI_CLOUD", "Gemini 2.5 Flash", DeckCyan),
                        Triple("OPENAI_CLOUD", "GPT-4o Mini", DeckEmerald),
                        Triple("LOCAL_AIDER", "Local CLI Assistant", DeckPurple)
                    ).forEach { (engineId, label, color) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        color = if (uiState.selectedEngine == engineId) color else DeckTextPrimary,
                                        fontWeight = if (uiState.selectedEngine == engineId) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectEngine(engineId)
                                engineMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Right Actions: API Key / Settings icon button & Clear chat icon button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Key / Settings icon button
                IconButton(
                    onClick = { viewModel.setShowApiKeyDialog(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .testTag("ai_settings_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Key Settings",
                        tint = if (uiState.geminiApiKey.isNotBlank() || uiState.openaiApiKey.isNotBlank()) DeckCyan else DeckTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Clear Chat button
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear conversation",
                        tint = DeckTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            val messages = uiState.session?.messages ?: emptyList()
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ask the AI Agent for command line pipelines, tool flags, or bash scripts.",
                            color = DeckTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            items(messages) { msg ->
                AgentMessageBubble(message = msg)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Bottom Input Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.session?.isGenerating == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = DeckCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Streaming response...",
                            color = DeckCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    TextButton(
                        onClick = { viewModel.stopGeneration() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            tint = DeckRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", color = DeckRed, fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = { Text("Ask about commands, ffmpeg filters, etc...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("agent_input_field"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary,
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (uiState.inputText.isNotBlank() && !uiState.isSending) DeckPurple else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .testTag("agent_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank() && !uiState.isSending) Color.White else DeckTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentMessageBubble(message: AgentMessage) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isUser = message.role == MessageRole.USER

    val bubbleBrush = if (isUser) {
        Brush.verticalGradient(
            listOf(
                DeckCyan.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUser) "YOU" else "COMMANDDECK AGENT",
                color = if (isUser) DeckCyan else DeckPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (!isUser && message.content.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = DeckTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(bubbleBrush)
                .border(
                    width = 1.dp,
                    color = if (isUser) DeckCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(14.dp)
        ) {
            SelectionContainer {
                MarkdownFormattedText(text = message.content)
            }
        }
    }
}

/**
 * Basic markdown parser to nicely render code blocks and text
 */
@Composable
fun MarkdownFormattedText(text: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (text.isEmpty()) {
        Text(text = "...", color = DeckTextMuted, fontSize = 13.sp)
        return
    }

    val parts = text.split("```")
    Column {
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 == 1) {
                // Code block
                val lines = part.trim().lines()
                val codeContent = if (lines.isNotEmpty() && (lines[0] == "bash" || lines[0] == "sh" || lines[0] == "python")) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part.trim()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Color(0xFF030712).copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = codeContent,
                            color = TerminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(codeContent))
                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = DeckCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Normal text
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        color = DeckTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeySettingsDialog(
    initialGeminiKey: String,
    initialOpenAiKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var geminiKey by remember { mutableStateOf(initialGeminiKey) }
    var openaiKey by remember { mutableStateOf(initialOpenAiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeckSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = DeckCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI Engine API Keys",
                    color = DeckTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Configure API keys to use cloud AI engines directly. Keys are saved securely on device.",
                    color = DeckTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Gemini Key
                Text(
                    text = "Google AI (Gemini) API Key",
                    color = DeckCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    placeholder = { Text("AIzaSy...", fontSize = 12.sp, color = DeckTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckCyan,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // OpenAI Key
                Text(
                    text = "OpenAI API Key",
                    color = DeckEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = openaiKey,
                    onValueChange = { openaiKey = it },
                    placeholder = { Text("sk-proj-...", fontSize = 12.sp, color = DeckTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openai_key_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckEmerald,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(geminiKey, openaiKey) },
                colors = ButtonDefaults.buttonColors(containerColor = DeckCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_api_keys_button")
            ) {
                Text("Save Keys", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeckTextSecondary)
            }
        }
    )
}
