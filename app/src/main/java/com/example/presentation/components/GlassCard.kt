package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeckBorderGlass
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.DeckSurfaceElevated

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = DeckBorderGlass,
    borderWidth: Dp = 1.dp,
    topGlowColor: Color? = null,
    topGlowFraction: Float = 1.0f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = Color.Transparent
    )

    val borderStroke = BorderStroke(
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(
                borderColor.copy(alpha = 0.22f),
                borderColor.copy(alpha = 0.06f)
            )
        )
    )

    val innerBackground = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.07f),
            Color(0xFF131C2E).copy(alpha = 0.65f),
            Color(0xFF0F172A).copy(alpha = 0.80f)
        )
    )

    val cardContent: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier
                .background(innerBackground)
                .padding(16.dp),
            content = content
        )

        // Glowing accent bar at top of card (like in Frosted Glass design for active jobs)
        if (topGlowColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(topGlowFraction)
                    .height(2.5.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                topGlowColor,
                                topGlowColor.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.clip(shape),
            shape = shape,
            colors = cardColors,
            border = borderStroke
        ) {
            Box(content = cardContent)
        }
    } else {
        Card(
            modifier = modifier.clip(shape),
            shape = shape,
            colors = cardColors,
            border = borderStroke
        ) {
            Box(content = cardContent)
        }
    }
}

