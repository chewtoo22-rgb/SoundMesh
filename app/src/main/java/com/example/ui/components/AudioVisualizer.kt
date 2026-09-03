package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicRose
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    rmsLevel: Float,
    frequencyBands: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    title: String = "LIVE AUDIO SPECTRUM",
    subtitle: String = "44.1 kHz PCM • Ultra-low Latency Mesh"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) SonicCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.2.sp
                    )
                }

                // VU Meter Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val segments = 10
                    val activeSegments = if (isPlaying) (rmsLevel * segments * 1.6f).toInt().coerceIn(1, segments) else 0
                    for (i in 0 until segments) {
                        val segmentColor = when {
                            i >= 8 -> SonicRose
                            i >= 6 -> SonicAmber
                            else -> SonicEmerald
                        }
                        val isLit = i < activeSegments
                        Box(
                            modifier = Modifier
                                .size(width = 5.dp, height = 12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isLit) segmentColor else ObsidianBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spectrum Wave Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val width = size.width
                val height = size.height
                val barCount = 28
                val barSpacing = 4f
                val totalSpacing = (barCount - 1) * barSpacing
                val barWidth = (width - totalSpacing) / barCount

                for (i in 0 until barCount) {
                    val bandIdx = (i * frequencyBands.size / barCount).coerceIn(0, frequencyBands.size - 1)
                    val rawEnergy = frequencyBands.getOrElse(bandIdx) { 0.1f }

                    val animatedHeight = if (isPlaying) {
                        val dynamicBoost = 0.2f + 0.8f * rawEnergy + 0.15f * sin(idlePhase + i * 0.4).toFloat()
                        (height * dynamicBoost.coerceIn(0.08f, 0.98f))
                    } else {
                        val idleWave = 0.08f + 0.05f * sin(idlePhase + i * 0.3).toFloat()
                        (height * idleWave)
                    }

                    val left = i * (barWidth + barSpacing)
                    val top = height - animatedHeight

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            SonicAmber,
                            SonicCyan,
                            SonicCyan.copy(alpha = 0.5f)
                        ),
                        startY = top,
                        endY = height
                    )

                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = if (isPlaying) "OUTPUT: ACTIVE" else "OUTPUT: IDLE",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPlaying) SonicCyan else TextSecondary
                )
            }
        }
    }
}
