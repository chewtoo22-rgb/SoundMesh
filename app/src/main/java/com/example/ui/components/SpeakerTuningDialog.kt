package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AudioProfile
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.SpatialZone
import com.example.model.SpeakerChannel
import com.example.model.SpeakerDevice
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicPurple
import com.example.ui.theme.SonicRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SpeakerTuningDialog(
    speaker: SpeakerDevice,
    onDismiss: () -> Unit,
    onApplyTuning: (
        zone: SpatialZone,
        profile: AudioProfile,
        equalizer: EqualizerSettings,
        channel: SpeakerChannel,
        fineTrimMs: Int
    ) -> Unit,
    onPingSpeaker: () -> Unit
) {
    // Local editable state
    var selectedZone by remember(speaker.id) { mutableStateOf(speaker.zone) }
    var selectedProfile by remember(speaker.id) { mutableStateOf(speaker.audioProfile) }
    var equalizerState by remember(speaker.id) { mutableStateOf(speaker.equalizer) }
    var selectedChannel by remember(speaker.id) { mutableStateOf(speaker.channel) }
    var fineTrimMs by remember(speaker.id) { mutableStateOf(speaker.fineTrimMs) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("speaker_tuning_dialog_${speaker.id}"),
            color = ObsidianBg,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(SonicCyan.copy(alpha = 0.4f))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(18.dp)
            ) {
                // Header Row
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
                                .background(SonicCyan.copy(alpha = 0.15f))
                                .border(1.dp, SonicCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SonicCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DSP TUNING & SPATIAL ZONE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                            Text(
                                text = speaker.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${speaker.deviceModel} • ${speaker.ipAddress} • ${speaker.batteryPercent}% Batt",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ping / Locate button
                        IconButton(
                            onClick = onPingSpeaker,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Chirp Speaker",
                                tint = SonicAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: Spatial Zone Assignment
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1. ASSIGN VIRTUAL SPATIAL ZONE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonicPurple,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = selectedZone.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getZoneColor(selectedZone),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Positions speaker inside the 3D room stage and sets channel pan",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Grid of 6 Zones
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ZoneOptionCard(
                                        zone = SpatialZone.LEFT_STAGE,
                                        isSelected = selectedZone == SpatialZone.LEFT_STAGE,
                                        onClick = {
                                            selectedZone = SpatialZone.LEFT_STAGE
                                            selectedChannel = SpeakerChannel.LEFT_ONLY
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ZoneOptionCard(
                                        zone = SpatialZone.CENTER_STAGE,
                                        isSelected = selectedZone == SpatialZone.CENTER_STAGE,
                                        onClick = {
                                            selectedZone = SpatialZone.CENTER_STAGE
                                            selectedChannel = SpeakerChannel.CENTER
                                            selectedProfile = AudioProfile.VOCAL_BOOST
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ZoneOptionCard(
                                        zone = SpatialZone.RIGHT_STAGE,
                                        isSelected = selectedZone == SpatialZone.RIGHT_STAGE,
                                        onClick = {
                                            selectedZone = SpatialZone.RIGHT_STAGE
                                            selectedChannel = SpeakerChannel.RIGHT_ONLY
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ZoneOptionCard(
                                        zone = SpatialZone.SUB_BASS,
                                        isSelected = selectedZone == SpatialZone.SUB_BASS,
                                        onClick = {
                                            selectedZone = SpatialZone.SUB_BASS
                                            selectedChannel = SpeakerChannel.SUBWOOFER
                                            selectedProfile = AudioProfile.BASS_BLAST
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ZoneOptionCard(
                                        zone = SpatialZone.REAR_SURROUND,
                                        isSelected = selectedZone == SpatialZone.REAR_SURROUND,
                                        onClick = {
                                            selectedZone = SpatialZone.REAR_SURROUND
                                            selectedChannel = SpeakerChannel.SURROUND_LEFT
                                            selectedProfile = AudioProfile.WIDE_STAGE
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ZoneOptionCard(
                                        zone = SpatialZone.ALL_PARTY,
                                        isSelected = selectedZone == SpatialZone.ALL_PARTY,
                                        onClick = {
                                            selectedZone = SpatialZone.ALL_PARTY
                                            selectedChannel = SpeakerChannel.STEREO_ALL
                                            selectedProfile = AudioProfile.ALL_STEREO
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Individual Audio Profile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. INDIVIDUAL AUDIO PROFILE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonicCyan,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = selectedProfile.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonicCyan,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dedicated hardware DSP tuning algorithm for this specific phone",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Profile selection chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioProfile.entries.forEach { profile ->
                                    val isSel = selectedProfile == profile
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SonicCyan.copy(alpha = 0.2f) else ObsidianCard)
                                            .border(
                                                1.dp,
                                                if (isSel) SonicCyan else ObsidianBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedProfile = profile }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("speaker_profile_${profile.name}")
                                    ) {
                                        Column {
                                            Text(
                                                text = profile.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSel) TextPrimary else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = profile.tag,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSel) SonicCyan else TextTertiary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: Individual 5-Band Equalizer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = SonicAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3. 5-BAND EQUALIZER",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SonicAmber,
                                        letterSpacing = 1.sp,
                                        fontSize = 11.sp
                                    )
                                }

                                Switch(
                                    checked = equalizerState.isEnabled,
                                    onCheckedChange = { equalizerState = equalizerState.copy(isEnabled = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SonicAmber,
                                        checkedTrackColor = SonicAmber.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextTertiary,
                                        uncheckedTrackColor = ObsidianBorder
                                    ),
                                    modifier = Modifier.size(width = 42.dp, height = 24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // EQ Presets Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EqualizerPreset.entries.forEach { preset ->
                                    val isPresetSel = equalizerState.preset == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isPresetSel) SonicAmber.copy(alpha = 0.2f) else ObsidianCard)
                                            .border(
                                                1.dp,
                                                if (isPresetSel) SonicAmber else ObsidianBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                equalizerState = EqualizerSettings.fromPreset(preset)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPresetSel) SonicAmber else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 5 Frequency Band Sliders (-12dB to +12dB)
                            val bands = listOf(
                                Triple("60 Hz", "Sub-Bass", equalizerState.band60Hz),
                                Triple("250 Hz", "Punch", equalizerState.band250Hz),
                                Triple("1 kHz", "Vocal Mid", equalizerState.band1kHz),
                                Triple("4 kHz", "Presence", equalizerState.band4kHz),
                                Triple("12 kHz", "Air / Treble", equalizerState.band12kHz)
                            )

                            bands.forEachIndexed { index, (freq, label, gain) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.width(72.dp)) {
                                        Text(
                                            text = freq,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextTertiary,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Slider(
                                        value = gain,
                                        onValueChange = { newGain ->
                                            equalizerState = when (index) {
                                                0 -> equalizerState.copy(band60Hz = newGain, preset = EqualizerPreset.CUSTOM)
                                                1 -> equalizerState.copy(band250Hz = newGain, preset = EqualizerPreset.CUSTOM)
                                                2 -> equalizerState.copy(band1kHz = newGain, preset = EqualizerPreset.CUSTOM)
                                                3 -> equalizerState.copy(band4kHz = newGain, preset = EqualizerPreset.CUSTOM)
                                                4 -> equalizerState.copy(band12kHz = newGain, preset = EqualizerPreset.CUSTOM)
                                                else -> equalizerState
                                            }
                                        },
                                        valueRange = -12f..12f,
                                        enabled = equalizerState.isEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = SonicAmber,
                                            activeTrackColor = SonicAmber,
                                            inactiveTrackColor = ObsidianBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    val sign = if (gain > 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format("%.1f", gain)} dB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (gain != 0f) SonicAmber else TextTertiary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.width(46.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 4: Channel Override & Micro-Latency Delay Trim
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "4. CHANNEL OVERRIDE & MICRO-DELAY",
                                style = MaterialTheme.typography.labelMedium,
                                color = SonicEmerald,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Channel chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SpeakerChannel.entries.forEach { ch ->
                                    val isChSel = selectedChannel == ch
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isChSel) SonicEmerald.copy(alpha = 0.2f) else ObsidianCard)
                                            .border(
                                                1.dp,
                                                if (isChSel) SonicEmerald else ObsidianBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedChannel = ch }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ch.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isChSel) SonicEmerald else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Latency Trim Slider (-50ms to +50ms)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phase Trim: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = fineTrimMs.toFloat(),
                                    onValueChange = { fineTrimMs = it.toInt() },
                                    valueRange = -50f..50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SonicEmerald,
                                        activeTrackColor = SonicEmerald,
                                        inactiveTrackColor = ObsidianBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${if (fineTrimMs > 0) "+" else ""}$fineTrimMs ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonicEmerald,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onApplyTuning(
                                selectedZone,
                                selectedProfile,
                                equalizerState,
                                selectedChannel,
                                fineTrimMs
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonicCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("apply_tuning_button")
                    ) {
                        Text("Apply & Sync to Speaker", color = ObsidianCardElevated)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneOptionCard(
    zone: SpatialZone,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneColor = getZoneColor(zone)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) zoneColor.copy(alpha = 0.2f) else ObsidianCard)
            .border(
                1.dp,
                if (isSelected) zoneColor else ObsidianBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = zone.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = zone.defaultChannel.shortName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) zoneColor else TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}
