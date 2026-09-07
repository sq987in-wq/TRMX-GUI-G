package com.example.presentation.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomTool
import com.example.model.ToolInputType
import com.example.ui.theme.DeckBackground
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckSurfaceElevated
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextPrimary
import com.example.ui.theme.DeckTextSecondary
import com.example.util.FileUtils

@Composable
fun ToolExecutionDialog(
    tool: CustomTool,
    onDismiss: () -> Unit,
    onExecute: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val inputValues = remember {
        mutableStateMapOf<String, String>().apply {
            tool.inputDefinitions.forEach { inputDef ->
                put(inputDef.key, inputDef.defaultValue)
            }
        }
    }

    val toolColor = try {
        Color(android.graphics.Color.parseColor(tool.accentColor))
    } catch (_: Exception) {
        DeckCyan
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF0F172A).copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(toolColor, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Run: ${tool.title}",
                        color = DeckTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tool.executable} • ${tool.category}",
                        color = toolColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (tool.description.isNotEmpty()) {
                    Text(
                        text = tool.description,
                        color = DeckTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (tool.inputDefinitions.isEmpty() || tool.inputType == "NONE") {
                    Text(
                        text = "This tool executes with preset parameters and requires no user arguments.",
                        color = DeckTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    tool.inputDefinitions.forEach { def ->
                        val isFileInput = def.type == ToolInputType.FILE ||
                                def.type == ToolInputType.FILE_PATH ||
                                tool.inputType == "FILE_PATH" ||
                                tool.inputType == "DIRECTORY"

                        val filePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri ->
                            uri?.let {
                                val localPath = FileUtils.resolveUriToLocalPath(context, it)
                                inputValues[def.key] = localPath
                            }
                        }

                        Text(
                            text = "${def.label} ${if (def.required) "*" else ""}",
                            color = DeckTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (isFileInput) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputValues[def.key] ?: "",
                                    onValueChange = { inputValues[def.key] = it },
                                    placeholder = { Text(def.placeholder.ifEmpty { "Select or enter file path..." }) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = toolColor,
                                        unfocusedBorderColor = DeckBorderGlass,
                                        focusedTextColor = DeckTextPrimary,
                                        unfocusedTextColor = DeckTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = toolColor),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, toolColor.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Browse file",
                                        modifier = Modifier.size(16.dp),
                                        tint = toolColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Browse", fontSize = 12.sp)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = inputValues[def.key] ?: "",
                                onValueChange = { inputValues[def.key] = it },
                                placeholder = {
                                    Text(
                                        when (def.type) {
                                            ToolInputType.URL -> "https://example.com/..."
                                            else -> def.placeholder.ifEmpty { "Enter value..." }
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = def.type != ToolInputType.TEXT,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = toolColor,
                                    unfocusedBorderColor = DeckBorderGlass,
                                    focusedTextColor = DeckTextPrimary,
                                    unfocusedTextColor = DeckTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Preview of template arguments
                val templateDisplay = if (tool.arguments.isNotEmpty()) {
                    tool.arguments.joinToString(" ")
                } else if (tool.argsTemplate.isNotEmpty()) {
                    tool.argsTemplate
                } else {
                    "(no extra arguments)"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeckSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Template: $templateDisplay",
                        color = DeckTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExecute(inputValues.toMap())
                },
                colors = ButtonDefaults.buttonColors(containerColor = toolColor)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DeckBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeckTextSecondary)
            }
        }
    )
}
