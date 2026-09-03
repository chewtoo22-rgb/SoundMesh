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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MeshState
import com.example.model.SpatialZone
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
fun SpatialZonesManagerCard(
    state: MeshState,
    onSelectZoneFilter: (SpatialZone?) -> Unit,
    onAdjustZoneVolume: (SpatialZone, Float) -> Unit,
    onToggleZoneMute: (SpatialZone) -> Unit,
    modifier: Modifier = Modifier
) {
    val speakers = state.connectedSpeakers
    val activeZone = state.selectedZoneFilter

    // Group speaker counts by zone
    val zoneCounts = SpatialZone.entries.associateWith { zone ->
        speakers.count { it.zone == zone }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spatial_zones_manager_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (activeZone != null) SonicPurple.copy(alpha = 0.5f) else ObsidianBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SonicPurple.copy(alpha = 0.15f))
                            .border(1.dp, SonicPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = SonicPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SPATIAL AUDIO ZONES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicPurple,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Virtual Acoustic Stage Layout",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Active zones pill
                val configuredZonesCount = zoneCounts.count { it.value > 0 }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SonicPurple.copy(alpha = 0.12f))
                        .border(1.dp, SonicPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$configuredZonesCount / 6 Active Zones",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonicPurple,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Virtual Stage Diagram
            Text(
                text = "INTERACTIVE ROOM STAGE • TAP ZONE TO MANAGE",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Stage Canvas Box: Front L / C / R + Sub + Rear Surrounds
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Front Stage (Left - Center - Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StageZoneTile(
                            zone = SpatialZone.LEFT_STAGE,
                            speakerCount = zoneCounts[SpatialZone.LEFT_STAGE] ?: 0,
                            isSelected = activeZone == SpatialZone.LEFT_STAGE,
                            accentColor = SonicCyan,
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.LEFT_STAGE) null else SpatialZone.LEFT_STAGE) },
                            modifier = Modifier.weight(1f)
                        )
                        StageZoneTile(
                            zone = SpatialZone.CENTER_STAGE,
                            speakerCount = zoneCounts[SpatialZone.CENTER_STAGE] ?: 0,
                            isSelected = activeZone == SpatialZone.CENTER_STAGE,
                            accentColor = SonicEmerald,
                            icon = Icons.Default.Mic,
                            onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.CENTER_STAGE) null else SpatialZone.CENTER_STAGE) },
                            modifier = Modifier.weight(1f)
                        )
                        StageZoneTile(
                            zone = SpatialZone.RIGHT_STAGE,
                            speakerCount = zoneCounts[SpatialZone.RIGHT_STAGE] ?: 0,
                            isSelected = activeZone == SpatialZone.RIGHT_STAGE,
                            accentColor = SonicCyan,
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.RIGHT_STAGE) null else SpatialZone.RIGHT_STAGE) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Middle / Low-End reinforcement (Subwoofer / Bass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StageZoneTile(
                            zone = SpatialZone.SUB_BASS,
                            speakerCount = zoneCounts[SpatialZone.SUB_BASS] ?: 0,
                            isSelected = activeZone == SpatialZone.SUB_BASS,
                            accentColor = SonicAmber,
                            icon = Icons.Default.GraphicEq,
                            onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.SUB_BASS) null else SpatialZone.SUB_BASS) },
                            modifier = Modifier.weight(1f)
                        )
                        StageZoneTile(
                            zone = SpatialZone.ALL_PARTY,
                            speakerCount = zoneCounts[SpatialZone.ALL_PARTY] ?: 0,
                            isSelected = activeZone == SpatialZone.ALL_PARTY,
                            accentColor = SonicPurple,
                            icon = Icons.Default.Speaker,
                            onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.ALL_PARTY) null else SpatialZone.ALL_PARTY) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Rear Surround Stage (Ambient 3D)
                    StageZoneTile(
                        zone = SpatialZone.REAR_SURROUND,
                        speakerCount = zoneCounts[SpatialZone.REAR_SURROUND] ?: 0,
                        isSelected = activeZone == SpatialZone.REAR_SURROUND,
                        accentColor = SonicRose,
                        icon = Icons.Default.SurroundSound,
                        onTap = { onSelectZoneFilter(if (activeZone == SpatialZone.REAR_SURROUND) null else SpatialZone.REAR_SURROUND) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Speakers chip
                ZoneChip(
                    title = "All Fleet (${speakers.size})",
                    isSelected = activeZone == null,
                    color = SonicCyan,
                    onClick = { onSelectZoneFilter(null) }
                )

                SpatialZone.entries.forEach { zone ->
                    val count = zoneCounts[zone] ?: 0
                    val zoneColor = getZoneColor(zone)
                    ZoneChip(
                        title = "${zone.displayName} ($count)",
                        isSelected = activeZone == zone,
                        color = zoneColor,
                        onClick = { onSelectZoneFilter(if (activeZone == zone) null else zone) }
                    )
                }
            }

            // Zone Master Controls (Appears when a zone is actively selected)
            AnimatedVisibility(visible = activeZone != null) {
                activeZone?.let { zone ->
                    val zoneSpeakers = speakers.filter { it.zone == zone }
                    val avgVolume = if (zoneSpeakers.isNotEmpty()) {
                        zoneSpeakers.map { it.volume }.average().toFloat()
                    } else 0.85f
                    val isZoneMuted = zoneSpeakers.isNotEmpty() && zoneSpeakers.all { it.isMuted }
                    val zoneColor = getZoneColor(zone)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(zoneColor.copy(alpha = 0.08f))
                            .border(1.dp, zoneColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ZONE MASTER • ${zone.displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = zoneColor,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${zoneSpeakers.size} Speakers Linked • ${zone.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // Dismiss filter button
                            IconButton(
                                onClick = { onSelectZoneFilter(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Filter",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Zone Volume Slider & Mute Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleZoneMute(zone) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isZoneMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Zone Mute",
                                    tint = if (isZoneMuted) SonicRose else zoneColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Slider(
                                value = if (isZoneMuted) 0f else avgVolume,
                                onValueChange = { onAdjustZoneVolume(zone, it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = zoneColor,
                                    activeTrackColor = zoneColor,
                                    inactiveTrackColor = ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("zone_volume_slider_${zone.id}")
                            )

                            Text(
                                text = "${((if (isZoneMuted) 0f else avgVolume) * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageZoneTile(
    zone: SpatialZone,
    speakerCount: Int,
    isSelected: Boolean,
    accentColor: Color,
    icon: ImageVector,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = speakerCount > 0
    val containerBg = if (isSelected) {
        accentColor.copy(alpha = 0.22f)
    } else if (isActive) {
        ObsidianCardElevated
    } else {
        ObsidianCard.copy(alpha = 0.5f)
    }

    val borderCol = if (isSelected) {
        accentColor
    } else if (isActive) {
        accentColor.copy(alpha = 0.4f)
    } else {
        ObsidianBorder.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("stage_zone_${zone.id}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive || isSelected) accentColor else TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = zone.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive || isSelected) TextPrimary else TextTertiary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isActive) accentColor else ObsidianBorder)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$speakerCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) ObsidianCardElevated else TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
                Text(
                    text = zone.defaultChannel.shortName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) accentColor else TextTertiary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun ZoneChip(
    title: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else ObsidianCard)
            .border(
                1.dp,
                if (isSelected) color else ObsidianBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp
        )
    }
}

fun getZoneColor(zone: SpatialZone): Color {
    return when (zone) {
        SpatialZone.LEFT_STAGE -> SonicCyan
        SpatialZone.CENTER_STAGE -> SonicEmerald
        SpatialZone.RIGHT_STAGE -> SonicCyan
        SpatialZone.REAR_SURROUND -> SonicRose
        SpatialZone.SUB_BASS -> SonicAmber
        SpatialZone.ALL_PARTY -> SonicPurple
    }
}
