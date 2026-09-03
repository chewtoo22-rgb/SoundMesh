package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioProfile
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.LatencyMode
import com.example.model.MeshState
import com.example.model.SoundQualityMode
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

/**
 * 1. Auto-Sync & Anti-Echo Calibration Deck
 */
@Composable
fun AutoSyncEchoCard(
    state: MeshState,
    onTriggerAutoSync: () -> Unit,
    onDelayOffsetChange: (Int) -> Unit,
    onTestSyncPulse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (state.isSyncCalibrated) SonicEmerald.copy(alpha = 0.6f) else SonicAmber.copy(alpha = 0.4f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (state.isSyncCalibrated) SonicEmerald else SonicAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ANTI-ECHO & CLOCK AUTO-SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isSyncCalibrated) SonicEmerald else SonicAmber,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (state.isSyncCalibrated) "Clocks Phase-Locked • Echo Eliminated" else "Echo Calibration Recommended",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Calibrated badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isSyncCalibrated) SonicEmerald.copy(alpha = 0.15f) else SonicAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.isSyncCalibrated) "CALIBRATED" else "DRIFT DETECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isSyncCalibrated) SonicEmerald else SonicAmber,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Aligns high-precision presentation timestamps (PTS) across all ${state.connectedSpeakers.size} phones to eliminate echo and acoustic comb filtering.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Auto-Sync Action Button
            Button(
                onClick = onTriggerAutoSync,
                enabled = !state.isAutoSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSyncCalibrated) SonicEmerald else SonicCyan,
                    contentColor = ObsidianCardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trigger_auto_sync_button")
            ) {
                if (state.isAutoSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ObsidianCardElevated
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Phase-locking clocks...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isSyncCalibrated) "Re-Sync All Speakers" else "One-Touch Auto-Sync All Speakers",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fine delay trim & transient pulse test
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
                        text = "Offset: ${state.syncDelayOffsetMs} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianCard)
                            .clickable { onDelayOffsetChange((state.syncDelayOffsetMs - 5).coerceAtLeast(0)) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("-5ms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianCard)
                            .clickable { onDelayOffsetChange((state.syncDelayOffsetMs + 5).coerceAtMost(300)) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+5ms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

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

/**
 * 2. Latency Mode & Network Buffer Deck
 */
@Composable
fun LatencyModeCard(
    currentMode: LatencyMode,
    onSelectMode: (LatencyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = SonicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LATENCY & BUFFER TARGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicCyan,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${currentMode.title} (~${currentMode.bufferTargetMs}ms)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Low delay socket indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SonicCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("IPTOS_LOWDELAY", fontSize = 9.sp, color = SonicCyan)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latency selector options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LatencyMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectMode(mode) }
                            .testTag("latency_mode_${mode.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ObsidianCardElevated else ObsidianCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) SonicCyan else ObsidianBorder
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) SonicCyan else TextPrimary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${mode.bufferTargetMs}ms buffer",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) TextSecondary else TextTertiary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 9.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Sound Quality & Bitrate Deck
 */
@Composable
fun SoundQualityCard(
    currentQuality: SoundQualityMode,
    onSelectQuality: (SoundQualityMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = SonicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SOUND QUALITY & BITRATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicCyan,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${currentQuality.title} • ${currentQuality.bitrateKbps} kbps",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoundQualityMode.entries.forEach { quality ->
                    val isSelected = currentQuality == quality
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectQuality(quality) }
                            .testTag("sound_quality_${quality.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ObsidianCardElevated else ObsidianCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) SonicCyan else ObsidianBorder
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = quality.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) SonicCyan else TextPrimary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${quality.sampleRate}Hz • 16b",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "${quality.bitrateKbps} kbps",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SonicEmerald else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Audio Profiles & Spatial Sound Enhancements (5.1 Surround, All Stereo, Bass Boost, Wide Stage)
 */
@Composable
fun AudioProfileCard(
    currentProfile: AudioProfile,
    onSelectProfile: (AudioProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SurroundSound,
                        contentDescription = null,
                        tint = SonicCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AUDIO PROFILES & SPATIAL ENHANCEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicCyan,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = currentProfile.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SonicCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (currentProfile == AudioProfile.SURROUND_5_1) "5.1 CINEMA" else "DSP ENHANCED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonicCyan,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AudioProfile.entries) { profile ->
                    val isSelected = currentProfile == profile
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SonicCyan else ObsidianCard)
                            .border(
                                1.dp,
                                if (isSelected) SonicCyan else ObsidianBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectProfile(profile) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("audio_profile_${profile.name}")
                    ) {
                        Column {
                            Text(
                                text = profile.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) ObsidianCardElevated else TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = profile.tag,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) ObsidianCardElevated.copy(alpha = 0.8f) else TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 5. 5-Band Graphic Equalizer with Presets Deck
 */
@Composable
fun EqualizerCard(
    settings: EqualizerSettings,
    onToggleEqualizer: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Enable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = if (settings.isEnabled) SonicCyan else TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "5-BAND GRAPHIC EQUALIZER",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.isEnabled) SonicCyan else TextTertiary,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Preset: ${settings.preset.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = onToggleEqualizer,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SonicCyan,
                        checkedTrackColor = SonicCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = ObsidianCard
                    ),
                    modifier = Modifier.testTag("toggle_equalizer_switch")
                )
            }

            AnimatedVisibility(visible = settings.isEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Preset selector row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(EqualizerPreset.entries) { preset ->
                            val isSelected = settings.preset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SonicCyan.copy(alpha = 0.2f) else ObsidianCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) SonicCyan else ObsidianBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("eq_preset_${preset.name}")
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SonicCyan else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5 Band Sliders
                    val bands = listOf(
                        Triple(0, "60Hz", settings.band60Hz),
                        Triple(1, "250Hz", settings.band250Hz),
                        Triple(2, "1kHz", settings.band1kHz),
                        Triple(3, "4kHz", settings.band4kHz),
                        Triple(4, "12kHz", settings.band12kHz)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        bands.forEach { (index, label, gain) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${if (gain > 0) "+" else ""}${gain.toInt()}dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (gain != 0f) SonicCyan else TextTertiary,
                                    fontSize = 10.sp
                                )

                                // Vertical slider or styled slider
                                Slider(
                                    value = gain,
                                    onValueChange = { onBandGainChange(index, it) },
                                    valueRange = -12f..12f,
                                    steps = 23,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SonicCyan,
                                        activeTrackColor = SonicCyan,
                                        inactiveTrackColor = ObsidianBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("eq_slider_band_$index")
                                )

                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
