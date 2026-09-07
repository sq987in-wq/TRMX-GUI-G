package com.example.presentation.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomTool
import com.example.model.InputType
import com.example.model.ToolInputDefinition
import com.example.ui.theme.DeckAmber
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
import java.util.UUID

@Composable
fun ToolEditorDialog(
    initialTool: CustomTool? = null,
    onDismiss: () -> Unit,
    onSave: (CustomTool) -> Unit
) {
    var name by remember { mutableStateOf(initialTool?.name ?: "") }
    var executable by remember { mutableStateOf(initialTool?.executable ?: "ffmpeg") }
    var description by remember { mutableStateOf(initialTool?.description ?: "") }
    var accentColor by remember { mutableStateOf(initialTool?.accentColor ?: "#06B6D4") }
    var argumentsString by remember { mutableStateOf(initialTool?.arguments?.joinToString(" ") ?: "-i {input} output/{output}") }

    val inputDefs = remember {
        mutableStateListOf<ToolInputDefinition>().apply {
            if (initialTool != null) {
                addAll(initialTool.inputDefinitions)
            } else {
                add(ToolInputDefinition(key = "input", label = "Source Path / URL", type = InputType.TEXT))
            }
        }
    }

    val palette = listOf(
        "#06B6D4" to "Cyan",
        "#A855F7" to "Purple",
        "#F59E0B" to "Amber",
        "#10B981" to "Emerald"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF0F172A).copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (initialTool == null) "Create Custom Action" else "Edit Action",
                color = DeckTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tool Name") },
                    placeholder = { Text("e.g., Extract Audio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckCyan,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Executable
                OutlinedTextField(
                    value = executable,
                    onValueChange = { executable = it },
                    label = { Text("Executable Binary (Whitelisted)") },
                    placeholder = { Text("ffmpeg, yt-dlp, apktool, aider, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckCyan,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckCyan,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Arguments Template
                OutlinedTextField(
                    value = argumentsString,
                    onValueChange = { argumentsString = it },
                    label = { Text("Arguments (Use {token} for inputs)") },
                    placeholder = { Text("-i {input} -c copy {output}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeckCyan,
                        unfocusedBorderColor = DeckBorderGlass,
                        focusedTextColor = DeckTextPrimary,
                        unfocusedTextColor = DeckTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Accent Color Selector
                Text("Accent Color", color = DeckTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { (hex, _) ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(col, CircleShape)
                                .border(
                                    width = if (accentColor == hex) 2.dp else 0.dp,
                                    color = if (accentColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { accentColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Definitions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DYNAMIC INPUT PARAMETERS",
                        color = DeckTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = {
                            inputDefs.add(
                                ToolInputDefinition(
                                    key = "param_${inputDefs.size + 1}",
                                    label = "Parameter ${inputDefs.size + 1}",
                                    type = InputType.TEXT
                                )
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add input", tint = DeckCyan)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                inputDefs.forEachIndexed { index, def ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = def.key,
                            onValueChange = { newKey ->
                                inputDefs[index] = def.copy(key = newKey)
                            },
                            label = { Text("Token") },
                            modifier = Modifier.weight(0.4f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeckCyan,
                                unfocusedBorderColor = DeckBorderGlass,
                                focusedTextColor = DeckTextPrimary,
                                unfocusedTextColor = DeckTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = def.label,
                            onValueChange = { newLabel ->
                                inputDefs[index] = def.copy(label = newLabel)
                            },
                            label = { Text("Label") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeckCyan,
                                unfocusedBorderColor = DeckBorderGlass,
                                focusedTextColor = DeckTextPrimary,
                                unfocusedTextColor = DeckTextPrimary
                            )
                        )
                        IconButton(
                            onClick = { inputDefs.removeAt(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = DeckRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || executable.isBlank()) return@Button
                    val argsList = argumentsString.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    val tool = CustomTool(
                        id = initialTool?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        icon = initialTool?.icon ?: "terminal",
                        accentColorHex = accentColor,
                        description = description.trim(),
                        inputDefinitions = inputDefs.toList(),
                        executable = executable.trim(),
                        arguments = argsList,
                        enabled = initialTool?.enabled ?: true,
                        sortOrder = initialTool?.sortOrder ?: 0,
                        createdAt = initialTool?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(tool)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeckCyan)
            ) {
                Text("Save Action", color = DeckBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeckTextSecondary)
            }
        }
    )
}
