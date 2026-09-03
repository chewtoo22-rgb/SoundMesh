package com.example.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.synth.SynthMode
import com.example.model.AudioProfile
import com.example.model.AudioSourceType
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.LatencyMode
import com.example.model.MeshState
import com.example.model.SoundQualityMode
import com.example.model.SpeakerChannel
import com.example.ui.components.AudioProfileCard
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.AutoSyncEchoCard
import com.example.ui.components.EqualizerCard
import com.example.ui.components.LatencyModeCard
import com.example.ui.components.SoundQualityCard
import com.example.ui.components.SpeakerCard
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
fun MasterControllerScreen(
    state: MeshState,
    onTogglePlay: () -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onMasterMuteToggle: () -> Unit,
    onSelectAudioSource: (AudioSourceType) -> Unit,
    onRequestSystemAudioCapture: () -> Unit,
    onSetSynthMode: (SynthMode) -> Unit,
    onTestSyncPulse: () -> Unit,
    onDelayOffsetChange: (Int) -> Unit,
    onTriggerAutoSync: () -> Unit = {},
    onSelectLatencyMode: (LatencyMode) -> Unit = {},
    onSelectSoundQuality: (SoundQualityMode) -> Unit = {},
    onSelectAudioProfile: (AudioProfile) -> Unit = {},
    onSelectEqualizerPreset: (EqualizerPreset) -> Unit = {},
    onUpdateEqualizerBand: (Int, Float) -> Unit = { _, _ -> },
    onToggleEqualizer: (Boolean) -> Unit = {},
    onSpeakerVolumeChange: (String, Float) -> Unit,
    onSpeakerMuteToggle: (String) -> Unit,
    onSpeakerChannelChange: (String, SpeakerChannel) -> Unit,
    onPingSpeaker: (String) -> Unit,
    onRemoveSpeaker: (String) -> Unit,
    onAddDemoSpeaker: () -> Unit,
    onSwitchToSpeakerMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPairingDialog by remember { mutableStateOf(false) }

    if (showPairingDialog) {
        PairingDialog(
            hostIp = state.hostIpAddress,
            port = 9876,
            connectedCount = state.connectedSpeakers.size,
            maxSpeakers = state.maxSpeakers,
            onDismiss = { showPairingDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("master_controller_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Network & Role Bar
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
                                .background(if (state.isPlaying) SonicCyan else SonicAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MASTER MEDIA CONTROLLER",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Host: ${state.hostIpAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Speaker count badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SonicCyan.copy(alpha = 0.15f))
                                .border(1.dp, SonicCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${state.connectedSpeakers.size}/${state.maxSpeakers} Speakers",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Switch to Speaker Mode button
                        IconButton(
                            onClick = onSwitchToSpeakerMode,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("switch_to_speaker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = "Switch to Speaker",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1.5. Master System Hardware Stats & Stream Info Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_telemetry_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row: Master Hardware & Wi-Fi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = SonicCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.localDeviceModel} (Host Phone)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        // Wi-Fi SSID
                        Text(
                            text = state.localWifiSsid,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Metric Chips: Battery, Wi-Fi RSSI, Master Volume, Fleet Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery
                        val battColor = when {
                            state.localBatteryPercent > 50 -> SonicEmerald
                            state.localBatteryPercent > 20 -> SonicAmber
                            else -> SonicRose
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.localIsCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = if (state.localIsCharging) SonicCyan else battColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("BATTERY", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 8.sp)
                                Text(
                                    text = if (state.localIsCharging) "⚡${state.localBatteryPercent}%" else "${state.localBatteryPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Wi-Fi Signal
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = SonicCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("SIGNAL", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 8.sp)
                                Text("${state.localWifiSignalDbm} dBm", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            }
                        }

                        // Master Volume
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isMasterMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (state.isMasterMuted) SonicRose else SonicAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("VOLUME", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 8.sp)
                                Text(if (state.isMasterMuted) "MUTED" else "${(state.masterVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = if (state.isMasterMuted) SonicRose else TextPrimary)
                            }
                        }

                        // Connected Fleet Health
                        val avgBattery = if (state.connectedSpeakers.isNotEmpty()) {
                            state.connectedSpeakers.map { it.batteryPercent }.average().toInt()
                        } else null
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = SonicEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("FLEET", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 8.sp)
                                Text(
                                    text = if (avgBattery != null) "🔋$avgBattery% avg" else "Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonicEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Now Playing Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianCard)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SonicCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NOW BROADCASTING: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicCyan,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${state.nowPlayingTrackTitle} • ${state.nowPlayingTrackArtist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Audio Visualizer Canvas
        item {
            AudioVisualizer(
                rmsLevel = state.liveRmsLevel,
                frequencyBands = state.liveFrequencyBands,
                isPlaying = state.isPlaying,
                title = "MASTER AUDIO STREAM • ${state.activeBitrateKbps} KBPS",
                subtitle = when (state.audioSource) {
                    AudioSourceType.SYSTEM_AUDIO -> if (state.isSystemCaptureActive) "Capturing all apps (Spotify, YouTube, Games)" else "System capture permission requested"
                    AudioSourceType.PARTY_BEATS -> "124 BPM Funk Beats & Acoustic Test Generator"
                    AudioSourceType.PARTY_MIC -> "Live Party Microphone / PA Megaphone"
                }
            )
        }

        // 3. Audio Source Selector (All Apps vs Beats vs Mic)
        item {
            val isSystemSelected = state.audioSource == AudioSourceType.SYSTEM_AUDIO
            val isBeatsSelected = state.audioSource == AudioSourceType.PARTY_BEATS
            val isMicSelected = state.audioSource == AudioSourceType.PARTY_MIC

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AUDIO BROADCAST SOURCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: All Apps (System Audio)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectAudioSource(AudioSourceType.SYSTEM_AUDIO) }
                            .testTag("source_system_audio"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemSelected) ObsidianCardElevated else ObsidianCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSystemSelected) SonicCyan else ObsidianBorder
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = null,
                                tint = if (isSystemSelected) SonicCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All Apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSystemSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Spotify/YT",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Option 2: Synthesizer & Beats
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectAudioSource(AudioSourceType.PARTY_BEATS) }
                            .testTag("source_party_beats"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBeatsSelected) ObsidianCardElevated else ObsidianCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isBeatsSelected) SonicCyan else ObsidianBorder
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isBeatsSelected) SonicCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Synth Beats",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isBeatsSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Built-in Test",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Option 3: Party Mic
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectAudioSource(AudioSourceType.PARTY_MIC) }
                            .testTag("source_party_mic"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMicSelected) ObsidianCardElevated else ObsidianCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isMicSelected) SonicCyan else ObsidianBorder
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isMicSelected) SonicCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Live Mic",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isMicSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Megaphone",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Sub-controls for System Audio Capture
                AnimatedVisibility(visible = isSystemSelected) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (state.isSystemCaptureActive) "Playback Capture Active" else "Audio Playback Capture",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.isSystemCaptureActive) SonicEmerald else TextPrimary
                                    )
                                    Text(
                                        text = "Captures Spotify, YouTube, Soundcloud & all apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = onRequestSystemAudioCapture,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isSystemCaptureActive) SonicEmerald.copy(alpha = 0.2f) else SonicCyan,
                                        contentColor = if (state.isSystemCaptureActive) SonicEmerald else ObsidianCardElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("request_capture_button")
                                ) {
                                    Text(if (state.isSystemCaptureActive) "Restart Capture" else "Grant Access")
                                }
                            }
                        }
                    }
                }

                // Sub-controls for Synth Beats
                AnimatedVisibility(visible = isBeatsSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSetSynthMode(SynthMode.FUNK_BEAT) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Funk Beat", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onSetSynthMode(SynthMode.SPATIAL_SWEEP) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stereo Sweep", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onSetSynthMode(SynthMode.ACOUSTIC_CLICK) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Click", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. Master Playback & Volume Deck
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Playback status and Main Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MASTER PLAYBACK CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (state.isPlaying) "Broadcasting to ${state.connectedSpeakers.size} Speakers" else "Playback Paused",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        // Big Play / Pause Button
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (state.isPlaying) SonicCyan else ObsidianCard)
                                .border(2.dp, SonicCyan, CircleShape)
                                .clickable { onTogglePlay() }
                                .testTag("master_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = if (state.isPlaying) ObsidianCardElevated else SonicCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Master Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMasterMuteToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("master_mute_button")
                        ) {
                            Icon(
                                imageVector = if (state.isMasterMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute Master",
                                tint = if (state.isMasterMuted) SonicRose else SonicCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Slider(
                            value = if (state.isMasterMuted) 0f else state.masterVolume,
                            onValueChange = onMasterVolumeChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = SonicCyan,
                                activeTrackColor = SonicCyan,
                                inactiveTrackColor = ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("master_volume_slider")
                        )

                        Text(
                            text = "${((if (state.isMasterMuted) 0f else state.masterVolume) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.width(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Delay Offset & Acoustic Pulse Test
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SonicAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mesh Sync Delay: ${state.syncDelayOffsetMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Micro adjust -5ms
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianCard)
                                    .clickable { onDelayOffsetChange((state.syncDelayOffsetMs - 5).coerceAtLeast(0)) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("-5ms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Micro adjust +5ms
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianCard)
                                    .clickable { onDelayOffsetChange((state.syncDelayOffsetMs + 5).coerceAtMost(300)) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+5ms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // Test Sync Pulse button
                            Button(
                                onClick = onTestSyncPulse,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SonicAmber.copy(alpha = 0.2f),
                                    contentColor = SonicAmber
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("test_sync_pulse_button")
                            ) {
                                Text("Sync Pulse", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Anti-Echo Auto-Sync Deck
        item {
            AutoSyncEchoCard(
                state = state,
                onTriggerAutoSync = onTriggerAutoSync,
                onDelayOffsetChange = onDelayOffsetChange,
                onTestSyncPulse = onTestSyncPulse
            )
        }

        // 6. Latency Mode & Buffer Target Deck
        item {
            LatencyModeCard(
                currentMode = state.latencyMode,
                onSelectMode = onSelectLatencyMode
            )
        }

        // 7. Sound Quality & Bitrate Deck
        item {
            SoundQualityCard(
                currentQuality = state.soundQuality,
                onSelectQuality = onSelectSoundQuality
            )
        }

        // 8. Audio Profiles & Spatial Sound Enhancements (5.1 Surround, All Stereo, etc.)
        item {
            AudioProfileCard(
                currentProfile = state.audioProfile,
                onSelectProfile = onSelectAudioProfile
            )
        }

        // 9. 5-Band Graphic Equalizer with Presets
        item {
            EqualizerCard(
                settings = state.equalizer,
                onToggleEqualizer = onToggleEqualizer,
                onSelectPreset = onSelectEqualizerPreset,
                onBandGainChange = onUpdateEqualizerBand
            )
        }

        // 10. Connected Satellite Speakers Header (Slots 1 to 10)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SATELLITE SPEAKERS (${state.connectedSpeakers.size} / ${state.maxSpeakers})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Arrange phones in room for spatial stereo sound",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Demo / Test speaker button (allows testing without needing 10 physical phones)
                    if (state.connectedSpeakers.size < state.maxSpeakers) {
                        OutlinedButton(
                            onClick = onAddDemoSpeaker,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_demo_speaker_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Slot", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Pair / QR Helper button
                    Button(
                        onClick = { showPairingDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SonicCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("pair_speakers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = ObsidianCardElevated,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pair Code", color = ObsidianCardElevated, fontSize = 11.sp)
                    }
                }
            }
        }

        // Empty state when 0 speakers connected
        if (state.connectedSpeakers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = SonicCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Satellite Speakers Connected Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect up to 10 Android phones on the same Wi-Fi or Hotspot to form your wireless speaker system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showPairingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SonicCyan)
                            ) {
                                Text("View Pairing Guide", color = ObsidianCardElevated)
                            }
                            OutlinedButton(
                                onClick = onAddDemoSpeaker
                            ) {
                                Text("Add Sample Phone")
                            }
                        }
                    }
                }
            }
        }

        // List of Connected Speakers (1 to 10)
        itemsIndexed(state.connectedSpeakers) { index, speaker ->
            SpeakerCard(
                slotIndex = index + 1,
                speaker = speaker,
                onVolumeChange = { vol -> onSpeakerVolumeChange(speaker.id, vol) },
                onMuteToggle = { onSpeakerMuteToggle(speaker.id) },
                onChannelChange = { ch -> onSpeakerChannelChange(speaker.id, ch) },
                onPingSpeaker = { onPingSpeaker(speaker.id) },
                onRemoveSpeaker = { onRemoveSpeaker(speaker.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
