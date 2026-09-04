package com.example.model

enum class DeviceRole {
    MASTER,
    SPEAKER
}

enum class AudioSourceType(val displayName: String, val subtitle: String) {
    SYSTEM_AUDIO("All Apps (System Audio)", "Capture Spotify, YouTube, Chrome, Games & Media"),
    PARTY_BEATS("Synthesizer & Beats", "Built-in party rhythms & acoustic test tones"),
    PARTY_MIC("Live Party Microphone", "Speak or sing into master phone as wireless PA")
}

enum class SpeakerChannel(val label: String, val shortName: String) {
    STEREO_ALL("Full Stereo (L + R)", "L+R"),
    LEFT_ONLY("Left Channel", "LEFT"),
    RIGHT_ONLY("Right Channel", "RIGHT"),
    CENTER("Center / Vocal", "CTR"),
    SURROUND_LEFT("Surround Left", "SUR-L"),
    SURROUND_RIGHT("Surround Right", "SUR-R"),
    SUBWOOFER("Subwoofer / Bass", "SUB"),
    MONO_MIX("Mono Mix", "MONO")
}

enum class LatencyMode(
    val title: String,
    val subtitle: String,
    val bufferTargetMs: Int,
    val samplesPerFrame: Int
) {
    ULTRA_LOW("Ultra-Low Latency", "15–20ms • Gaming, Live Mic & PA (Minimal lag)", 18, 256),
    BALANCED("Balanced Sync", "35–45ms • Spotify, YouTube, Movies (Anti-echo)", 40, 512),
    STABLE("Rock-Solid Mesh", "80–100ms • Large parties, crowded Wi-Fi (No dropouts)", 90, 1024)
}

enum class SoundQualityMode(
    val title: String,
    val subtitle: String,
    val sampleRate: Int,
    val bitrateKbps: Int
) {
    STUDIO_HIFI("Studio Hi-Fi Master", "44.1 kHz 16-bit uncompressed PCM (1,411 kbps)", 44100, 1411),
    BALANCED_CD("Balanced High Quality", "44.1 kHz dynamic low-jitter streaming (1,411 kbps)", 44100, 1411),
    BANDWIDTH_SAVER("Bandwidth Saver", "Optimized packet pacing for weak Wi-Fi", 44100, 705)
}

enum class AudioProfile(
    val title: String,
    val description: String,
    val tag: String
) {
    ALL_STEREO("All Stereo (Party Wall)", "Every phone outputs full stereo for room-filling sound", "STEREO"),
    SURROUND_5_1("5.1 Surround Sound", "Simulates 5.1 theatre: Front L/R, Center, Surrounds & Subwoofer", "5.1 3D"),
    BASS_BLAST("Bass Blast & Limiter", "Punchy low-end harmonic enhancement without speaker distortion", "BASS+"),
    VOCAL_BOOST("Vocal & Dialogue Clarity", "Amplifies vocals and speech; ideal for karaoke and podcasts", "VOCAL"),
    WIDE_STAGE("Wide Soundstage", "Mid-Side 3D psychoacoustic stereo widening across the room", "WIDE")
}

enum class EqualizerPreset(val displayName: String) {
    FLAT("Flat / Natural"),
    BASS_BOOST("Bass Boost"),
    VOCAL_CLEAR("Vocal Clarity"),
    ELECTRONIC("Club / Dance"),
    ROCK_POP("Rock / Pop"),
    ACOUSTIC("Acoustic Warmth"),
    CUSTOM("Custom EQ")
}

data class EqualizerSettings(
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val band60Hz: Float = 0f,   // -12dB to +12dB
    val band250Hz: Float = 0f,  // -12dB to +12dB
    val band1kHz: Float = 0f,   // -12dB to +12dB
    val band4kHz: Float = 0f,   // -12dB to +12dB
    val band12kHz: Float = 0f,  // -12dB to +12dB
    val isEnabled: Boolean = true
) {
    companion object {
        fun fromPreset(preset: EqualizerPreset): EqualizerSettings {
            return when (preset) {
                EqualizerPreset.FLAT -> EqualizerSettings(preset = preset, 0f, 0f, 0f, 0f, 0f)
                EqualizerPreset.BASS_BOOST -> EqualizerSettings(preset = preset, 7f, 4.5f, 0f, 1f, 2f)
                EqualizerPreset.VOCAL_CLEAR -> EqualizerSettings(preset = preset, -2f, 0f, 3.5f, 5f, 2.5f)
                EqualizerPreset.ELECTRONIC -> EqualizerSettings(preset = preset, 6.5f, 3.5f, -1.5f, 3.5f, 5.5f)
                EqualizerPreset.ROCK_POP -> EqualizerSettings(preset = preset, 4f, 2f, 1f, 3f, 4f)
                EqualizerPreset.ACOUSTIC -> EqualizerSettings(preset = preset, 2.5f, 2f, 1f, 2.5f, 3.5f)
                EqualizerPreset.CUSTOM -> EqualizerSettings(preset = preset)
            }
        }
    }
}

data class MasterSystemStats(
    val deviceName: String = "Master Host Phone",
    val deviceModel: String = "Samsung Galaxy",
    val ipAddress: String = "127.0.0.1",
    val batteryPercent: Int = 88,
    val isCharging: Boolean = false,
    val wifiSignalDbm: Int = -50,
    val wifiSignalLevel: Int = 4, // 0 to 4 bars
    val wifiSsid: String = "SoundMesh Wi-Fi",
    val wifiLinkSpeedMbps: Int = 433,
    val masterVolumePercent: Int = 85,
    val isMuted: Boolean = false,
    val nowPlayingTitle: String = "Spotify / All Apps System Audio",
    val nowPlayingArtist: String = "44.1 kHz 16-bit PCM • Low-Latency Stream",
    val isPlaying: Boolean = false,
    val audioProfile: AudioProfile = AudioProfile.ALL_STEREO,
    val latencyMode: LatencyMode = LatencyMode.BALANCED,
    val activeBitrateKbps: Int = 1411,
    val connectedSpeakersCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SpatialZone(
    val id: String,
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val defaultChannel: SpeakerChannel,
    val defaultProfile: AudioProfile = AudioProfile.ALL_STEREO
) {
    LEFT_STAGE("left_stage", "Left Stage", "LEFT", "Stage Left & Front-Left placement", SpeakerChannel.LEFT_ONLY),
    CENTER_STAGE("center_stage", "Center Stage", "CENTER", "Vocal clarity, lead dialogue & focus", SpeakerChannel.CENTER, AudioProfile.VOCAL_BOOST),
    RIGHT_STAGE("right_stage", "Right Stage", "RIGHT", "Stage Right & Front-Right placement", SpeakerChannel.RIGHT_ONLY),
    REAR_SURROUND("rear_surround", "Rear Surround", "SURROUND", "Ambient 3D surround sound", SpeakerChannel.SURROUND_LEFT, AudioProfile.WIDE_STAGE),
    SUB_BASS("sub_bass", "Sub / Bass Zone", "SUB/BASS", "Dedicated low-end bass reinforcement", SpeakerChannel.SUBWOOFER, AudioProfile.BASS_BLAST),
    ALL_PARTY("all_party", "Whole Room", "PARTY", "All-around stereo party room fill", SpeakerChannel.STEREO_ALL, AudioProfile.ALL_STEREO)
}

data class SpeakerDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 9876,
    val channel: SpeakerChannel = SpeakerChannel.STEREO_ALL,
    val zone: SpatialZone = SpatialZone.ALL_PARTY,
    val audioProfile: AudioProfile = AudioProfile.ALL_STEREO,
    val equalizer: EqualizerSettings = EqualizerSettings(),
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val latencyMs: Long = 18,
    val fineTrimMs: Int = 0,
    val batteryPercent: Int = 88,
    val isCharging: Boolean = false,
    val wifiSignalDbm: Int = -52,
    val wifiSignalLevel: Int = 4,
    val deviceModel: String = "Satellite Phone",
    val isConnected: Boolean = true,
    val isSyncLocked: Boolean = true,
    val bufferHealthPercent: Int = 98,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class MeshState(
    val role: DeviceRole = DeviceRole.MASTER,
    val isPlaying: Boolean = false,
    val masterVolume: Float = 0.85f,
    val isMasterMuted: Boolean = false,
    val audioSource: AudioSourceType = AudioSourceType.SYSTEM_AUDIO,
    val connectedSpeakers: List<SpeakerDevice> = emptyList(),
    val maxSpeakers: Int = 10,
    val syncDelayOffsetMs: Int = 40,
    val latencyMode: LatencyMode = LatencyMode.BALANCED,
    val soundQuality: SoundQualityMode = SoundQualityMode.STUDIO_HIFI,
    val audioProfile: AudioProfile = AudioProfile.ALL_STEREO,
    val equalizer: EqualizerSettings = EqualizerSettings(),
    val isAutoSyncing: Boolean = false,
    val isSyncCalibrated: Boolean = true,
    val localChannel: SpeakerChannel = SpeakerChannel.STEREO_ALL,
    val localZone: SpatialZone = SpatialZone.ALL_PARTY,
    val localEqualizer: EqualizerSettings = EqualizerSettings(),
    val selectedZoneFilter: SpatialZone? = null,
    val localVolume: Float = 0.90f,
    val localLatencyTrimMs: Int = 0,
    val isConnectedToMaster: Boolean = false,
    val connectedMasterIp: String? = null,
    val hostIpAddress: String = "127.0.0.1",
    val isHotspotActive: Boolean = false,
    val isSystemCaptureActive: Boolean = false,
    val liveRmsLevel: Float = 0.0f,
    val liveFrequencyBands: List<Float> = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.3f, 0.2f, 0.1f, 0.05f),
    val statusMessage: String = "Ready to connect up to 10 speaker phones",
    val isAudioEngineRunning: Boolean = false,
    val packetsTransferred: Long = 0,
    val activeBitrateKbps: Int = 1411,
    val masterStats: MasterSystemStats = MasterSystemStats(),
    val localBatteryPercent: Int = 88,
    val localIsCharging: Boolean = false,
    val localWifiSignalDbm: Int = -50,
    val localWifiSignalLevel: Int = 4,
    val localWifiSsid: String = "SoundMesh Wi-Fi",
    val localDeviceModel: String = "Android Device",
    val nowPlayingTrackTitle: String = "Spotify / All Apps System Audio",
    val nowPlayingTrackArtist: String = "All Apps Audio Capture (44.1 kHz PCM)"
)
