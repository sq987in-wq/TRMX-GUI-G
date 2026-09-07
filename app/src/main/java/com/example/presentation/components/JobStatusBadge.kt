package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JobStatus
import com.example.ui.theme.DeckAmber
import com.example.ui.theme.DeckCyan
import com.example.ui.theme.DeckEmerald
import com.example.ui.theme.DeckRed
import com.example.ui.theme.DeckTextMuted

@Composable
fun JobStatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (status) {
        JobStatus.QUEUED -> DeckAmber to "QUEUED"
        JobStatus.RUNNING -> DeckCyan to "RUNNING"
        JobStatus.COMPLETED -> DeckEmerald to "COMPLETED"
        JobStatus.FAILED -> DeckRed to "FAILED"
        JobStatus.CANCELLED -> DeckTextMuted to "CANCELLED"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dotAlpha = if (status == JobStatus.RUNNING) alphaAnim else 1.0f

    Row(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(dotAlpha)
                .background(color = statusColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
