package com.example.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckTextMuted
import com.example.ui.theme.DeckTextSecondary
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalText

@Composable
fun TerminalConsole(
    stdout: List<String>,
    stderr: List<String>,
    title: String = "LIVE OUTPUT",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val combinedLines = stdout.map { false to it } + stderr.map { true to it }

    LaunchedEffect(combinedLines.size) {
        if (combinedLines.isNotEmpty()) {
            listState.animateScrollToItem(combinedLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DeckTextMuted.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
    ) {
        // Window Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    TerminalBackground.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(9.dp).background(DeckRed, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(9.dp).background(TerminalGreen, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(9.dp).background(TerminalText, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = DeckTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val fullLog = (stdout + stderr).joinToString("\n")
                    clipboardManager.setText(AnnotatedString(fullLog))
                    Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy log",
                    tint = DeckTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Output lines
        if (combinedLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting process stream...",
                    color = DeckTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(combinedLines) { (isError, line) ->
                    Text(
                        text = line,
                        color = if (isError) TerminalRed else TerminalText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
