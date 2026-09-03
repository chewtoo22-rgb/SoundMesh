package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.SpatialZone
import com.example.model.SpeakerChannel
import com.example.model.SpeakerDevice
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
fun SpeakerCard(
    slotIndex: Int,
    speaker: SpeakerDevice,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onChannelChange: (SpeakerChannel) -> Unit,
    onZoneChange: (SpatialZone) -> Unit = {},
    onOpenTuning: () -> Unit = {},
    onPingSpeaker: () -> Unit,
    onRemoveSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChannelMenuOpen by remember { mutableStateOf(false) }
    var isZoneMenuOpen by remember { mutableStateOf(false) }
    val zoneColor = getZoneColor(speaker.zone)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speaker_card_${speaker.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ObsidianCardElevated
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (speaker.isMuted) ObsidianBorder else zoneColor.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Slot badge, Name, IP, Zone & Channel Tag, Tune, Ping, Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Slot index badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(zoneColor.copy(alpha = 0.15f))
                            .border(1.dp, zoneColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$slotIndex",
                            style = MaterialTheme.typography.labelSmall,
                            color = zoneColor,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = speaker.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${speaker.deviceModel} • ${speaker.ipAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Zone Selector Pill
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(zoneColor.copy(alpha = 0.15f))
                                .border(1.dp, zoneColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { isZoneMenuOpen = true }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("zone_selector_${speaker.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = speaker.zone.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = zoneColor,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isZoneMenuOpen,
                            onDismissRequest = { isZoneMenuOpen = false }
                        ) {
                            SpatialZone.entries.forEach { zone ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(zone.displayName, style = MaterialTheme.typography.bodyMedium)
                                            Text(zone.subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                        }
                                    },
                                    onClick = {
                                        onZoneChange(zone)
                                        isZoneMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Channel selector pill
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianCard)
                                .border(1.dp, SonicCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { isChannelMenuOpen = true }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("channel_selector_${speaker.id}")
                        ) {
                            Text(
                                text = speaker.channel.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = SonicCyan,
                                fontSize = 10.sp
                            )
                        }

                        DropdownMenu(
                            expanded = isChannelMenuOpen,
                            onDismissRequest = { isChannelMenuOpen = false }
                        ) {
                            SpeakerChannel.entries.forEach { channel ->
                                DropdownMenuItem(
                                    text = { Text(channel.label) },
                                    onClick = {
                                        onChannelChange(channel)
                                        isChannelMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Tune & Enhance Button
                    IconButton(
                        onClick = onOpenTuning,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tune_speaker_${speaker.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tune & Enhance",
                            tint = SonicCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ping / Identify button
                    IconButton(
                        onClick = onPingSpeaker,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("ping_speaker_${speaker.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Chirp Speaker",
                            tint = SonicAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Remove
                    IconButton(
                        onClick = onRemoveSpeaker,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("remove_speaker_${speaker.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Speaker",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Acoustic DSP Badges Row: Profile, Equalizer, Phase Trim
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zone description badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(zoneColor.copy(alpha = 0.12f))
                        .clickable { onOpenTuning() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = speaker.zone.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = zoneColor,
                        fontSize = 10.sp
                    )
                }

                // Audio Profile badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SonicCyan.copy(alpha = 0.12f))
                        .clickable { onOpenTuning() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = speaker.audioProfile.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = SonicCyan,
                        fontSize = 10.sp
                    )
                }

                // Equalizer preset badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SonicAmber.copy(alpha = 0.12f))
                        .clickable { onOpenTuning() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = SonicAmber,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "EQ: ${speaker.equalizer.preset.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicAmber,
                            fontSize = 10.sp
                        )
                    }
                }

                if (speaker.fineTrimMs != 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SonicEmerald.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (speaker.fineTrimMs > 0) "+" else ""}${speaker.fineTrimMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicEmerald,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Chips: Battery, Wi-Fi Signal, Latency, Volume & Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery
                val batteryColor = when {
                    speaker.batteryPercent > 50 -> SonicEmerald
                    speaker.batteryPercent > 20 -> SonicAmber
                    else -> SonicRose
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (speaker.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = null,
                        tint = if (speaker.isCharging) SonicCyan else batteryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (speaker.isCharging) "⚡${speaker.batteryPercent}%" else "${speaker.batteryPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Wi-Fi Signal
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = SonicCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${speaker.wifiSignalDbm} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Latency
                val latencyColor = when {
                    speaker.latencyMs < 30 -> SonicEmerald
                    speaker.latencyMs < 80 -> SonicAmber
                    else -> SonicRose
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = latencyColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${speaker.latencyMs} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = latencyColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Volume and Sync state
                Text(
                    text = if (speaker.isMuted) "MUTED" else "${(speaker.volume * 100).toInt()}% SYNCED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (speaker.isMuted) SonicRose else SonicEmerald,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Volume Slider + Mute Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mute_speaker_${speaker.id}")
                ) {
                    Icon(
                        imageVector = if (speaker.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (speaker.isMuted) SonicRose else SonicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Slider(
                    value = if (speaker.isMuted) 0f else speaker.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = zoneColor,
                        activeTrackColor = zoneColor,
                        inactiveTrackColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider_${speaker.id}")
                )

                Text(
                    text = "${((if (speaker.isMuted) 0f else speaker.volume) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(38.dp)
                )
            }
        }
    }
}
