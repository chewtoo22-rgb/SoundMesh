package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MeshState
import com.example.model.SpeakerChannel
import com.example.ui.components.AudioVisualizer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SpeakerReceiverScreen(
    state: MeshState,
    onLocalVolumeChange: (Float) -> Unit,
    onLocalChannelChange: (SpeakerChannel) -> Unit,
    onLatencyTrimChange: (Int) -> Unit,
    onConnectToMasterIp: (String) -> Unit,
    onSwitchToMasterMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customIpInput by remember { mutableStateOf(state.connectedMasterIp ?: "") }
    var isManualConnectOpen by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "speaker_cone")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("speaker_receiver_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar with Mode Switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isConnectedToMaster) SonicEmerald else SonicAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SATELLITE WIRELESS SPEAKER",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (state.isConnectedToMaster) "Connected to Master: ${state.connectedMasterIp}" else "Listening on Port 9876",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Switch back to Master Mode
                    Button(
                        onClick = onSwitchToMasterMode,
                        colors = ButtonDefaults.buttonColors(containerColor = SonicCyan.copy(alpha = 0.2f), contentColor = SonicCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("switch_to_master_button")
                    ) {
                        Text("Make Master", fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. MASTER PHONE LIVE TELEMETRY & STATS (Broadcaster System Stats)
        item {
            val master = state.masterStats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_stats_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (state.isConnectedToMaster) SonicCyan.copy(alpha = 0.45f) else ObsidianBorder
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Title Row: Master Device Model, IP, Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SonicCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Master Host Phone",
                                    tint = SonicCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = master?.deviceModel ?: "Master Host Phone",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${master?.ipAddress ?: (state.connectedMasterIp ?: "127.0.0.1")} • ${master?.wifiSsid ?: "Wi-Fi Mesh"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Live Sync Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.isConnectedToMaster) SonicEmerald.copy(alpha = 0.15f) else SonicAmber.copy(alpha = 0.15f))
                                .border(1.dp, if (state.isConnectedToMaster) SonicEmerald.copy(alpha = 0.5f) else SonicAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.isConnectedToMaster) "MASTER LINKED" else "SEARCHING",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isConnectedToMaster) SonicEmerald else SonicAmber,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Master Telemetry Metric Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Master Battery
                        val battVal = master?.batteryPercent ?: 88
                        val isCharging = master?.isCharging ?: false
                        val battColor = when {
                            battVal > 50 -> SonicEmerald
                            battVal > 20 -> SonicAmber
                            else -> SonicRose
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                    contentDescription = null,
                                    tint = if (isCharging) SonicCyan else battColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BATTERY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isCharging) "⚡$battVal%" else "$battVal%",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isCharging) "Charging" else "On Battery",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        // 2. Master Wi-Fi Signal
                        val wifiDbm = master?.wifiSignalDbm ?: -48
                        val wifiSpeed = master?.wifiLinkSpeedMbps ?: 433
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = SonicCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SIGNAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$wifiDbm dBm",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "$wifiSpeed Mbps",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        // 3. Master Volume
                        val masterVol = master?.masterVolumePercent ?: (state.masterVolume * 100).toInt()
                        val isMuted = master?.isMuted ?: state.isMasterMuted
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isMuted) SonicRose else SonicAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VOLUME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isMuted) "MUTED" else "$masterVol%",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isMuted) SonicRose else TextPrimary
                            )
                            Text(
                                text = if (isMuted) "Silenced" else "Broadcast",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Now Playing Info Box from Master
                    val nowTitle = master?.nowPlayingTitle ?: state.nowPlayingTrackTitle
                    val nowArtist = master?.nowPlayingArtist ?: state.nowPlayingTrackArtist
                    val isMasterPlaying = master?.isPlaying ?: state.isPlaying
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCard)
                            .border(1.dp, SonicCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SonicCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Now Playing",
                                        tint = SonicCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "NOW PLAYING FROM MASTER",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SonicCyan,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isMasterPlaying) SonicEmerald else SonicAmber)
                                        )
                                    }
                                    Text(
                                        text = nowTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = nowArtist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Fleet count & Quality badge
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SonicCyan.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${master?.connectedSpeakersCount ?: 1} Phones in Mesh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonicCyan,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${master?.activeBitrateKbps ?: 1411} kbps PCM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Local Phone Stats Strip (Satellite Phone Telemetry)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THIS PHONE: ${state.localDeviceModel} • 🔋${if (state.localIsCharging) "⚡" else ""}${state.localBatteryPercent}% • 📶 ${state.localWifiSignalDbm} dBm • 🔊 ${(state.localVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 2. Big Animated Speaker HUD & Acoustic Pulse Cone
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (state.isConnectedToMaster) SonicCyan.copy(alpha = 0.35f) else ObsidianBorder
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing Concentric Speaker Waves Canvas
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .testTag("speaker_cone_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val maxRadius = size.width / 2
                            val dynamicRms = if (state.isPlaying) (state.liveRmsLevel * 40f) else 0f

                            // Outer acoustic rings
                            for (i in 1..4) {
                                val radius = (maxRadius * (i / 4.0f) * pulseScale + dynamicRms).coerceIn(10f, maxRadius)
                                val alpha = (1.0f - (i / 4.5f)).coerceIn(0.1f, 0.8f)
                                drawCircle(
                                    color = SonicCyan.copy(alpha = alpha * 0.4f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Glowing Inner Speaker Cone Core
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SonicAmber,
                                        SonicCyan,
                                        ObsidianCard
                                    ),
                                    center = center,
                                    radius = maxRadius * 0.45f
                                ),
                                radius = maxRadius * 0.45f,
                                center = center
                            )
                        }

                        // Center Speaker Icon
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = "Speaker Cone",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (state.isConnectedToMaster) "SPEAKER ONLINE • PHASE-LOCKED" else "SEARCHING FOR MASTER PHONE...",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isConnectedToMaster) SonicEmerald else SonicAmber,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Current Channel: ${state.localChannel.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sync & Profile Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SonicEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ZERO-ECHO PTS SYNC",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicEmerald,
                                fontSize = 9.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SonicCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = state.audioProfile.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                fontSize = 9.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianBorder)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${state.latencyMode.bufferTargetMs}ms BUFFER",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Audio Visualizer Spectrum
        item {
            AudioVisualizer(
                rmsLevel = state.liveRmsLevel,
                frequencyBands = state.liveFrequencyBands,
                isPlaying = state.isPlaying || state.liveRmsLevel > 0.05f,
                title = "RECEIVER AUDIO FEED • ${state.localChannel.shortName}",
                subtitle = "Active Jitter Buffer: ${40 + state.localLatencyTrimMs} ms"
            )
        }

        // 4. Spatial Channel Assignment Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SPATIAL CHANNEL ASSIGNMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel selector pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpeakerChannel.entries.forEach { channel ->
                            val isSelected = state.localChannel == channel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SonicCyan else ObsidianCardElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) SonicCyan else ObsidianBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onLocalChannelChange(channel) }
                                    .padding(vertical = 8.dp)
                                    .testTag("select_channel_${channel.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channel.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) ObsidianCardElevated else TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Local Volume & Room Acoustic Distance Calibration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = SonicCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Speaker Output Volume", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Text("${(state.localVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = SonicCyan)
                    }

                    Slider(
                        value = state.localVolume,
                        onValueChange = onLocalVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = SonicCyan,
                            activeTrackColor = SonicCyan,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("local_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Latency / Distance Fine Trim
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SonicAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Acoustic Distance Trim", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Compensates sound travel delay (1ms / ft)", style = MaterialTheme.typography.bodySmall, color = TextTertiary, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "${if (state.localLatencyTrimMs > 0) "+" else ""}${state.localLatencyTrimMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SonicAmber
                        )
                    }

                    Slider(
                        value = state.localLatencyTrimMs.toFloat(),
                        onValueChange = { onLatencyTrimChange(it.toInt()) },
                        valueRange = -50f..50f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = SonicAmber,
                            activeTrackColor = SonicAmber,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("latency_trim_slider")
                    )
                }
            }
        }

        // 6. Manual Master IP Connection (if router blocks mDNS)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Direct IP Connect",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { isManualConnectOpen = !isManualConnectOpen },
                            modifier = Modifier.testTag("toggle_direct_ip_button")
                        ) {
                            Icon(
                                imageVector = if (isManualConnectOpen) Icons.Default.Tune else Icons.Default.Wifi,
                                contentDescription = "Manual IP",
                                tint = SonicCyan
                            )
                        }
                    }

                    if (isManualConnectOpen) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customIpInput,
                                onValueChange = { customIpInput = it },
                                label = { Text("Master Host IP (e.g. 192.168.1.5)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SonicCyan,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("master_ip_input_field")
                            )

                            Button(
                                onClick = { onConnectToMasterIp(customIpInput.trim()) },
                                colors = ButtonDefaults.buttonColors(containerColor = SonicCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("connect_ip_button")
                            ) {
                                Text("Connect", color = ObsidianCardElevated)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
