package com.example

import com.example.model.AudioProfile
import com.example.model.LatencyMode
import com.example.model.MasterSystemStats
import com.example.model.SpeakerChannel
import com.example.model.SpeakerDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun masterStats_creationAndDefaults() {
        val stats = MasterSystemStats(
            deviceName = "Galaxy S24 Ultra",
            deviceModel = "SM-S928B",
            ipAddress = "192.168.1.10",
            batteryPercent = 92,
            isCharging = true,
            wifiSignalDbm = -45,
            wifiLinkSpeedMbps = 866,
            masterVolumePercent = 80,
            isMuted = false,
            nowPlayingTitle = "Synthwave Odyssey",
            nowPlayingArtist = "Mesh Master Radio",
            connectedSpeakersCount = 4,
            latencyMode = LatencyMode.ULTRA_LOW,
            audioProfile = AudioProfile.SURROUND_5_1
        )

        assertEquals("Galaxy S24 Ultra", stats.deviceName)
        assertEquals("SM-S928B", stats.deviceModel)
        assertEquals(92, stats.batteryPercent)
        assertTrue(stats.isCharging)
        assertEquals(-45, stats.wifiSignalDbm)
        assertEquals(LatencyMode.ULTRA_LOW, stats.latencyMode)
        assertEquals(AudioProfile.SURROUND_5_1, stats.audioProfile)
    }

    @Test
    fun speakerDevice_telemetryFields() {
        val speaker = SpeakerDevice(
            id = "speaker-001",
            name = "Rear Right Satellite",
            deviceModel = "Pixel 8 Pro",
            ipAddress = "192.168.1.25",
            channel = SpeakerChannel.SURROUND_RIGHT,
            batteryPercent = 78,
            isCharging = false,
            wifiSignalDbm = -52,
            wifiSignalLevel = 4,
            latencyMs = 18,
            volume = 0.85f
        )

        assertEquals("speaker-001", speaker.id)
        assertEquals("Pixel 8 Pro", speaker.deviceModel)
        assertEquals(78, speaker.batteryPercent)
        assertFalse(speaker.isCharging)
        assertEquals(-52, speaker.wifiSignalDbm)
        assertEquals(18, speaker.latencyMs)
        assertEquals(0.85f, speaker.volume, 0.001f)
    }
}
