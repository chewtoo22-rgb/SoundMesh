package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.projection.MediaProjection
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.capture.SystemAudioCaptureManager
import com.example.audio.mesh.MeshAudioBroadcaster
import com.example.audio.mesh.MeshAudioReceiver
import com.example.audio.synth.SynthMode
import com.example.model.AudioProfile
import com.example.model.AudioSourceType
import com.example.model.DeviceRole
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.LatencyMode
import com.example.model.MasterSystemStats
import com.example.model.MeshState
import com.example.model.SoundQualityMode
import com.example.model.SpatialZone
import com.example.model.SpeakerChannel
import com.example.model.SpeakerDevice
import com.example.util.SystemStatsProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID

class SoundMeshViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MeshState())
    val state: StateFlow<MeshState> = _state.asStateFlow()

    private var broadcaster: MeshAudioBroadcaster? = null
    private var receiver: MeshAudioReceiver? = null
    private var audioCaptureManager: SystemAudioCaptureManager? = null

    private var activeMediaProjection: MediaProjection? = null

    init {
        detectLocalIpAddress()
        startLocalTelemetryLoop()
        setupMasterMode()
    }

    private fun startLocalTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    val telemetry = SystemStatsProvider.getTelemetry(getApplication())
                    _state.update {
                        it.copy(
                            localBatteryPercent = telemetry.batteryPercent,
                            localIsCharging = telemetry.isCharging,
                            localWifiSignalDbm = telemetry.wifiSignalDbm,
                            localWifiSignalLevel = telemetry.wifiSignalLevel,
                            localWifiSsid = telemetry.wifiSsid,
                            localDeviceModel = telemetry.deviceModel
                        )
                    }
                } catch (_: Exception) {}
                delay(2000)
            }
        }
    }

    private fun detectLocalIpAddress() {
        viewModelScope.launch {
            var foundIp = "127.0.0.1"
            try {
                // Check Wi-Fi Manager first
                val wifiManager = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiIp = wifiManager?.connectionInfo?.ipAddress
                if (wifiIp != null && wifiIp != 0) {
                    @Suppress("DEPRECATION")
                    foundIp = Formatter.formatIpAddress(wifiIp)
                } else {
                    // Fallback to network interfaces (works for Wi-Fi Hotspot)
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val intf = interfaces.nextElement()
                        val addrs = intf.inetAddresses
                        while (addrs.hasMoreElements()) {
                            val addr = addrs.nextElement()
                            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                                foundIp = addr.hostAddress ?: "127.0.0.1"
                                break
                            }
                        }
                        if (foundIp != "127.0.0.1") break
                    }
                }
            } catch (e: Exception) {
                Log.w("SoundMeshVM", "IP detection error: ${e.message}")
            }

            _state.update { it.copy(hostIpAddress = foundIp) }
        }
    }

    fun setMediaProjection(projection: MediaProjection) {
        activeMediaProjection = projection
        _state.update { it.copy(isSystemCaptureActive = true, statusMessage = "Capturing System Audio") }

        if (_state.value.role == DeviceRole.MASTER && _state.value.audioSource == AudioSourceType.SYSTEM_AUDIO) {
            startSystemCapture()
        }
    }

    fun selectRole(role: DeviceRole) {
        if (_state.value.role == role) return

        if (role == DeviceRole.MASTER) {
            setupMasterMode()
        } else {
            setupSpeakerMode()
        }
    }

    private fun setupMasterMode() {
        receiver?.stop()
        receiver = null

        broadcaster?.stop()
        broadcaster = MeshAudioBroadcaster(
            context = getApplication(),
            onSpeakerDiscovered = { speaker ->
                viewModelScope.launch {
                    _state.update { current ->
                        val existingList = current.connectedSpeakers.toMutableList()
                        val idx = existingList.indexOfFirst { it.id == speaker.id }
                        if (idx >= 0) {
                            existingList[idx] = speaker
                        } else if (existingList.size < current.maxSpeakers) {
                            existingList.add(speaker)
                        }
                        current.copy(connectedSpeakers = existingList)
                    }
                }
            },
            onSpeakerLatencyUpdated = { speakerId, latency ->
                viewModelScope.launch {
                    _state.update { current ->
                        val updated = current.connectedSpeakers.map {
                            if (it.id == speakerId) it.copy(latencyMs = latency) else it
                        }
                        current.copy(connectedSpeakers = updated)
                    }
                }
            },
            onAudioLevelUpdated = { rms, bands ->
                _state.update {
                    it.copy(liveRmsLevel = rms, liveFrequencyBands = bands)
                }
            }
        ).apply {
            hostIpAddress = _state.value.hostIpAddress
            nowPlayingCustomTitle = _state.value.nowPlayingTrackTitle
            nowPlayingCustomArtist = _state.value.nowPlayingTrackArtist
            masterVolume = _state.value.masterVolume
            isMasterMuted = _state.value.isMasterMuted
            activeAudioSource = _state.value.audioSource
            start()
        }

        _state.update {
            it.copy(
                role = DeviceRole.MASTER,
                isAudioEngineRunning = true,
                statusMessage = "Master Controller Active"
            )
        }

        if (_state.value.isPlaying && _state.value.audioSource == AudioSourceType.SYSTEM_AUDIO) {
            startSystemCapture()
        }
    }

    private fun setupSpeakerMode() {
        broadcaster?.stop()
        broadcaster = null
        audioCaptureManager?.stopCapture()

        receiver?.stop()
        receiver = MeshAudioReceiver(
            context = getApplication(),
            onConnectedToMaster = { masterIp ->
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            isConnectedToMaster = true,
                            connectedMasterIp = masterIp,
                            statusMessage = "Connected to Master: $masterIp"
                        )
                    }
                }
            },
            onAudioLevelUpdated = { rms, bands ->
                _state.update {
                    it.copy(liveRmsLevel = rms, liveFrequencyBands = bands)
                }
            },
            onChirpReceived = {
                viewModelScope.launch {
                    _state.update { it.copy(statusMessage = "🔔 Locator Chirp Received!") }
                }
            },
            onMasterStatsReceived = { stats ->
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            masterStats = stats,
                            isConnectedToMaster = true,
                            connectedMasterIp = stats.ipAddress
                        )
                    }
                }
            },
            onSpeakerTuningReceived = { zone, profile, eq, channel, trimMs, volume, isMuted ->
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            localZone = zone,
                            audioProfile = profile,
                            localEqualizer = eq,
                            localChannel = channel,
                            localLatencyTrimMs = trimMs,
                            localVolume = if (isMuted) 0f else volume,
                            statusMessage = "DSP: Assigned to ${zone.displayName} • ${profile.title}"
                        )
                    }
                }
            }
        ).apply {
            currentChannel = _state.value.localChannel
            localVolume = _state.value.localVolume
            localLatencyTrimMs = _state.value.localLatencyTrimMs
            start(_state.value.connectedMasterIp)
        }

        _state.update {
            it.copy(
                role = DeviceRole.SPEAKER,
                isAudioEngineRunning = true,
                statusMessage = "Satellite Speaker Ready"
            )
        }
    }

    fun togglePlay() {
        val newPlayState = !_state.value.isPlaying
        _state.update { it.copy(isPlaying = newPlayState) }

        broadcaster?.setPlaying(newPlayState)

        if (newPlayState) {
            when (_state.value.audioSource) {
                AudioSourceType.SYSTEM_AUDIO -> startSystemCapture()
                AudioSourceType.PARTY_MIC -> startMicCapture()
                AudioSourceType.PARTY_BEATS -> audioCaptureManager?.stopCapture()
            }
        } else {
            audioCaptureManager?.stopCapture()
        }
    }

    fun setMasterVolume(vol: Float) {
        _state.update { it.copy(masterVolume = vol) }
        broadcaster?.masterVolume = vol
    }

    fun toggleMasterMute() {
        val newMute = !_state.value.isMasterMuted
        _state.update { it.copy(isMasterMuted = newMute) }
        broadcaster?.isMasterMuted = newMute
    }

    fun selectAudioSource(source: AudioSourceType) {
        val (title, artist) = when (source) {
            AudioSourceType.SYSTEM_AUDIO -> Pair("Spotify / System Audio Capture", "All Apps Media (44.1 kHz PCM)")
            AudioSourceType.PARTY_BEATS -> Pair("Synthesizer & Party Beats", "Algorithmic DSP Beats")
            AudioSourceType.PARTY_MIC -> Pair("Live Wireless Microphone", "Ultra-Low-Latency Mic Feed")
        }
        _state.update {
            it.copy(
                audioSource = source,
                nowPlayingTrackTitle = title,
                nowPlayingTrackArtist = artist
            )
        }
        broadcaster?.activeAudioSource = source
        broadcaster?.nowPlayingCustomTitle = title
        broadcaster?.nowPlayingCustomArtist = artist

        if (_state.value.isPlaying) {
            when (source) {
                AudioSourceType.SYSTEM_AUDIO -> startSystemCapture()
                AudioSourceType.PARTY_MIC -> startMicCapture()
                AudioSourceType.PARTY_BEATS -> audioCaptureManager?.stopCapture()
            }
        }
    }

    fun updateNowPlayingInfo(title: String, artistOrApp: String) {
        _state.update {
            it.copy(
                nowPlayingTrackTitle = title,
                nowPlayingTrackArtist = artistOrApp
            )
        }
        broadcaster?.nowPlayingCustomTitle = title
        broadcaster?.nowPlayingCustomArtist = artistOrApp
    }

    fun setSynthMode(mode: SynthMode) {
        broadcaster?.setSynthMode(mode)
        _state.update { it.copy(statusMessage = "Synth Beat Mode: ${mode.name}") }
    }

    private fun startSystemCapture() {
        val projection = activeMediaProjection
        if (projection == null) {
            _state.update { it.copy(statusMessage = "Tap 'Grant Access' for System Audio capture") }
            return
        }

        audioCaptureManager?.stopCapture()
        audioCaptureManager = SystemAudioCaptureManager(
            onPcmData = { data, len, rms, bands ->
                broadcaster?.feedCapturedPcm(data, len)
                _state.update { it.copy(liveRmsLevel = rms, liveFrequencyBands = bands) }
            },
            onError = { msg ->
                _state.update { it.copy(statusMessage = msg) }
            }
        )
        audioCaptureManager?.startCapture(AudioSourceType.SYSTEM_AUDIO, projection)
    }

    private fun startMicCapture() {
        audioCaptureManager?.stopCapture()
        audioCaptureManager = SystemAudioCaptureManager(
            onPcmData = { data, len, rms, bands ->
                broadcaster?.feedCapturedPcm(data, len)
                _state.update { it.copy(liveRmsLevel = rms, liveFrequencyBands = bands) }
            },
            onError = { msg ->
                _state.update { it.copy(statusMessage = msg) }
            }
        )
        audioCaptureManager?.startCapture(AudioSourceType.PARTY_MIC)
    }

    fun setSyncDelayOffset(offsetMs: Int) {
        _state.update { it.copy(syncDelayOffsetMs = offsetMs) }
        broadcaster?.syncDelayOffsetMs = offsetMs
    }

    /**
     * Auto-Sync: Measures round-trip ping and clock drift across all connected speaker phones,
     * calculates the optimal jitter delay headroom, and aligns all playback presentation clocks
     * to eliminate echoes.
     */
    fun triggerAutoSyncCalibration() {
        viewModelScope.launch {
            _state.update { it.copy(isAutoSyncing = true, statusMessage = "Aligning all speaker clocks & eliminating echo...") }
            val calibratedDelay = broadcaster?.calibrateAndAlignAllSpeakers() ?: _state.value.syncDelayOffsetMs
            kotlinx.coroutines.delay(400) // Brief animation delay for feedback
            _state.update {
                it.copy(
                    isAutoSyncing = false,
                    isSyncCalibrated = true,
                    syncDelayOffsetMs = calibratedDelay,
                    statusMessage = "All ${it.connectedSpeakers.size} phones phase-locked & echo-calibrated ($calibratedDelay ms)"
                )
            }
        }
    }

    fun setLatencyMode(mode: LatencyMode) {
        _state.update {
            it.copy(
                latencyMode = mode,
                syncDelayOffsetMs = mode.bufferTargetMs,
                statusMessage = "Switched to ${mode.title} (${mode.bufferTargetMs}ms buffer)"
            )
        }
        broadcaster?.latencyMode = mode
        broadcaster?.syncDelayOffsetMs = mode.bufferTargetMs
        broadcaster?.broadcastConfigUpdate()
    }

    fun setSoundQuality(quality: SoundQualityMode) {
        _state.update {
            it.copy(
                soundQuality = quality,
                activeBitrateKbps = quality.bitrateKbps,
                statusMessage = "Sound Quality: ${quality.title} (${quality.bitrateKbps} kbps)"
            )
        }
        broadcaster?.soundQuality = quality
    }

    fun setAudioProfile(profile: AudioProfile) {
        _state.update { it.copy(audioProfile = profile, statusMessage = "Audio Enhancement: ${profile.title}") }
        broadcaster?.audioProfile = profile
        receiver?.setAudioProfile(profile)

        // If 5.1 surround was selected, update local state list to reflect auto-assigned roles
        if (profile == AudioProfile.SURROUND_5_1) {
            _state.update { current ->
                val updated = current.connectedSpeakers.mapIndexed { index, speaker ->
                    val ch = when (index) {
                        0 -> SpeakerChannel.CENTER
                        1 -> SpeakerChannel.LEFT_ONLY
                        2 -> SpeakerChannel.RIGHT_ONLY
                        3 -> SpeakerChannel.SURROUND_LEFT
                        4 -> SpeakerChannel.SURROUND_RIGHT
                        5 -> SpeakerChannel.SUBWOOFER
                        else -> SpeakerChannel.STEREO_ALL
                    }
                    speaker.copy(channel = ch)
                }
                current.copy(connectedSpeakers = updated)
            }
        } else if (profile == AudioProfile.ALL_STEREO) {
            _state.update { current ->
                val updated = current.connectedSpeakers.map { it.copy(channel = SpeakerChannel.STEREO_ALL) }
                current.copy(connectedSpeakers = updated)
            }
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        val newSettings = EqualizerSettings.fromPreset(preset)
        _state.update { it.copy(equalizer = newSettings, statusMessage = "EQ Preset: ${preset.displayName}") }
        broadcaster?.updateEqualizer(newSettings)
        receiver?.updateEqualizer(newSettings)
    }

    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) {
        val current = _state.value.equalizer
        val updated = when (bandIndex) {
            0 -> current.copy(band60Hz = gainDb, preset = EqualizerPreset.CUSTOM)
            1 -> current.copy(band250Hz = gainDb, preset = EqualizerPreset.CUSTOM)
            2 -> current.copy(band1kHz = gainDb, preset = EqualizerPreset.CUSTOM)
            3 -> current.copy(band4kHz = gainDb, preset = EqualizerPreset.CUSTOM)
            4 -> current.copy(band12kHz = gainDb, preset = EqualizerPreset.CUSTOM)
            else -> current
        }
        _state.update { it.copy(equalizer = updated) }
        broadcaster?.updateEqualizer(updated)
        receiver?.updateEqualizer(updated)
    }

    fun toggleEqualizer(enabled: Boolean) {
        val updated = _state.value.equalizer.copy(isEnabled = enabled)
        _state.update { it.copy(equalizer = updated) }
        broadcaster?.updateEqualizer(updated)
        receiver?.updateEqualizer(updated)
    }

    fun fineTuneSpeakerLatency(speakerId: String, trimMs: Int) {
        _state.update { current ->
            val updated = current.connectedSpeakers.map {
                if (it.id == speakerId) it.copy(fineTrimMs = trimMs) else it
            }
            current.copy(connectedSpeakers = updated)
        }
    }

    fun testSyncPulse() {
        // Trigger a synchronized transient click across all phones
        _state.update { it.copy(statusMessage = "🔊 Testing Sync Pulse across all speakers") }
        broadcaster?.let { b ->
            // Briefly broadcast click mode
            setSynthMode(SynthMode.ACOUSTIC_CLICK)
        }
    }

    fun updateSpeakerVolume(speakerId: String, volume: Float) {
        _state.update { current ->
            val updated = current.connectedSpeakers.map {
                if (it.id == speakerId) it.copy(volume = volume) else it
            }
            current.copy(connectedSpeakers = updated)
        }
        broadcaster?.updateSpeakerVolume(speakerId, volume, false)
    }

    fun toggleSpeakerMute(speakerId: String) {
        _state.update { current ->
            val updated = current.connectedSpeakers.map {
                if (it.id == speakerId) it.copy(isMuted = !it.isMuted) else it
            }
            current.copy(connectedSpeakers = updated)
        }
        val spk = _state.value.connectedSpeakers.firstOrNull { it.id == speakerId }
        if (spk != null) {
            broadcaster?.updateSpeakerVolume(speakerId, spk.volume, spk.isMuted)
        }
    }

    fun setZoneFilter(zone: SpatialZone?) {
        _state.update { it.copy(selectedZoneFilter = zone) }
    }

    fun assignSpeakerToZone(speakerId: String, zone: SpatialZone) {
        var updatedSpeaker: SpeakerDevice? = null
        _state.update { current ->
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.id == speakerId) {
                    val modified = spk.copy(
                        zone = zone,
                        channel = zone.defaultChannel,
                        audioProfile = if (spk.audioProfile == AudioProfile.ALL_STEREO) zone.defaultProfile else spk.audioProfile
                    )
                    updatedSpeaker = modified
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = "Assigned to ${zone.displayName} (${zone.defaultChannel.shortName})"
            )
        }
        updatedSpeaker?.let { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun updateSpeakerAudioProfile(speakerId: String, profile: AudioProfile) {
        var updatedSpeaker: SpeakerDevice? = null
        _state.update { current ->
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.id == speakerId) {
                    val modified = spk.copy(audioProfile = profile)
                    updatedSpeaker = modified
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = "Profile: ${profile.title}"
            )
        }
        updatedSpeaker?.let { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun updateSpeakerEqualizer(speakerId: String, eq: EqualizerSettings) {
        var updatedSpeaker: SpeakerDevice? = null
        _state.update { current ->
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.id == speakerId) {
                    val modified = spk.copy(equalizer = eq)
                    updatedSpeaker = modified
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = "EQ updated: ${eq.preset.displayName}"
            )
        }
        updatedSpeaker?.let { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun updateSpeakerTuning(
        speakerId: String,
        zone: SpatialZone,
        profile: AudioProfile,
        eq: EqualizerSettings,
        channel: SpeakerChannel,
        fineTrimMs: Int
    ) {
        var updatedSpeaker: SpeakerDevice? = null
        _state.update { current ->
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.id == speakerId) {
                    val modified = spk.copy(
                        zone = zone,
                        audioProfile = profile,
                        equalizer = eq,
                        channel = channel,
                        fineTrimMs = fineTrimMs
                    )
                    updatedSpeaker = modified
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = "Tuning applied to speaker #${speakerId.take(4)}"
            )
        }
        updatedSpeaker?.let { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun adjustZoneVolume(zone: SpatialZone, volume: Float) {
        val affected = mutableListOf<SpeakerDevice>()
        _state.update { current ->
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.zone == zone) {
                    val modified = spk.copy(volume = volume)
                    affected.add(modified)
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = "${zone.displayName} volume set to ${(volume * 100).toInt()}%"
            )
        }
        affected.forEach { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun toggleZoneMute(zone: SpatialZone) {
        val affected = mutableListOf<SpeakerDevice>()
        _state.update { current ->
            val isAnyUnmuted = current.connectedSpeakers.any { it.zone == zone && !it.isMuted }
            val newMute = isAnyUnmuted
            val updatedList = current.connectedSpeakers.map { spk ->
                if (spk.zone == zone) {
                    val modified = spk.copy(isMuted = newMute)
                    affected.add(modified)
                    modified
                } else spk
            }
            current.copy(
                connectedSpeakers = updatedList,
                statusMessage = if (newMute) "${zone.displayName} Muted" else "${zone.displayName} Unmuted"
            )
        }
        affected.forEach { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun updateSpeakerChannel(speakerId: String, channel: SpeakerChannel) {
        var updatedSpeaker: SpeakerDevice? = null
        _state.update { current ->
            val updated = current.connectedSpeakers.map {
                if (it.id == speakerId) {
                    val modified = it.copy(channel = channel)
                    updatedSpeaker = modified
                    modified
                } else it
            }
            current.copy(connectedSpeakers = updated)
        }
        broadcaster?.updateSpeakerChannel(speakerId, channel)
        updatedSpeaker?.let { broadcaster?.sendSpeakerTuningUpdate(it) }
    }

    fun pingSpeaker(speakerId: String) {
        broadcaster?.sendChirp(speakerId)
        _state.update { it.copy(statusMessage = "Chirping Speaker #$speakerId") }
    }

    fun removeSpeaker(speakerId: String) {
        _state.update { current ->
            val updated = current.connectedSpeakers.filterNot { it.id == speakerId }
            current.copy(connectedSpeakers = updated)
        }
        broadcaster?.removeSpeaker(speakerId)
    }

    private data class DemoSpec(
        val zone: SpatialZone,
        val channel: SpeakerChannel,
        val profile: AudioProfile,
        val eqPreset: EqualizerPreset
    )

    fun addDemoSpeaker() {
        val currentList = _state.value.connectedSpeakers
        if (currentList.size >= _state.value.maxSpeakers) return

        val slot = currentList.size + 1
        val sampleNames = listOf(
            "Galaxy S24 Ultra", "Pixel 8 Pro", "OnePlus 12", "Xiaomi 14 Pro",
            "Sony Xperia 1 VI", "Motorola Edge 50", "Galaxy Tab S9", "Pixel 7a",
            "Asus ROG Phone 8", "Nothing Phone (2)"
        )
        val name = sampleNames.getOrElse(slot - 1) { "Android Phone #$slot" }
        val fakeIp = "192.168.1.${100 + slot}"

        val spec = when (slot) {
            1 -> DemoSpec(SpatialZone.LEFT_STAGE, SpeakerChannel.LEFT_ONLY, AudioProfile.ALL_STEREO, EqualizerPreset.BASS_BOOST)
            2 -> DemoSpec(SpatialZone.RIGHT_STAGE, SpeakerChannel.RIGHT_ONLY, AudioProfile.ALL_STEREO, EqualizerPreset.ROCK_POP)
            3 -> DemoSpec(SpatialZone.CENTER_STAGE, SpeakerChannel.CENTER, AudioProfile.VOCAL_BOOST, EqualizerPreset.VOCAL_CLEAR)
            4 -> DemoSpec(SpatialZone.SUB_BASS, SpeakerChannel.SUBWOOFER, AudioProfile.BASS_BLAST, EqualizerPreset.BASS_BOOST)
            5 -> DemoSpec(SpatialZone.REAR_SURROUND, SpeakerChannel.SURROUND_LEFT, AudioProfile.WIDE_STAGE, EqualizerPreset.ACOUSTIC)
            6 -> DemoSpec(SpatialZone.REAR_SURROUND, SpeakerChannel.SURROUND_RIGHT, AudioProfile.WIDE_STAGE, EqualizerPreset.ACOUSTIC)
            else -> DemoSpec(SpatialZone.ALL_PARTY, SpeakerChannel.STEREO_ALL, AudioProfile.ALL_STEREO, EqualizerPreset.FLAT)
        }

        val newSpeaker = SpeakerDevice(
            id = UUID.randomUUID().toString().take(6),
            name = name,
            ipAddress = fakeIp,
            port = 9876,
            channel = spec.channel,
            zone = spec.zone,
            audioProfile = spec.profile,
            equalizer = EqualizerSettings.fromPreset(spec.eqPreset),
            volume = 0.85f,
            latencyMs = (12 + (slot * 3)).toLong(),
            batteryPercent = (95 - (slot * 4)).coerceIn(40, 100)
        )

        _state.update { it.copy(connectedSpeakers = currentList + newSpeaker) }
        broadcaster?.registeredSpeakers?.put(newSpeaker.id, newSpeaker)
    }

    fun setLocalSpeakerVolume(volume: Float) {
        _state.update { it.copy(localVolume = volume) }
        receiver?.localVolume = volume
    }

    fun setLocalSpeakerChannel(channel: SpeakerChannel) {
        _state.update { it.copy(localChannel = channel) }
        receiver?.currentChannel = channel
    }

    fun setLocalLatencyTrim(trimMs: Int) {
        _state.update { it.copy(localLatencyTrimMs = trimMs) }
        receiver?.localLatencyTrimMs = trimMs
    }

    fun connectToMasterIp(ip: String) {
        val normalizedIp = ip.trim()
        val isValidIp = try {
            InetAddress.getByName(normalizedIp)
            normalizedIp.isNotEmpty()
        } catch (_: Exception) {
            false
        }

        if (!isValidIp) {
            _state.update {
                it.copy(
                    isConnectedToMaster = false,
                    statusMessage = "Enter a valid master IP address"
                )
            }
            return
        }

        _state.update {
            it.copy(
                connectedMasterIp = normalizedIp,
                isConnectedToMaster = false,
                statusMessage = "Connecting to Master: $normalizedIp"
            )
        }
        receiver?.masterIpAddress = normalizedIp
    }

    override fun onCleared() {
        super.onCleared()
        broadcaster?.stop()
        receiver?.stop()
        audioCaptureManager?.stopCapture()
        activeMediaProjection?.stop()
    }
}
