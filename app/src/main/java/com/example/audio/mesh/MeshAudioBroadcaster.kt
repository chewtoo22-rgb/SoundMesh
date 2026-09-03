package com.example.audio.mesh

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.audio.dsp.AudioDspEngine
import com.example.audio.synth.SynthesizerAudioGenerator
import com.example.model.AudioProfile
import com.example.model.AudioSourceType
import com.example.model.EqualizerSettings
import com.example.model.LatencyMode
import com.example.model.SoundQualityMode
import com.example.model.SpeakerChannel
import com.example.model.SpeakerDevice
import com.example.util.SystemStatsProvider
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt

class MeshAudioBroadcaster(
    private val context: Context,
    private val onSpeakerDiscovered: (SpeakerDevice) -> Unit,
    private val onSpeakerLatencyUpdated: (String, Long) -> Unit,
    private val onAudioLevelUpdated: (Float, List<Float>) -> Unit
) {
    private val isRunning = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)
    private val sequenceNumber = AtomicLong(0)
    private val pendingSyncPings = ConcurrentHashMap<String, Long>()

    private var broadcastSocket: DatagramSocket? = null
    private var multicastSocket: MulticastSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var multicastAddress: InetAddress? = null

    private var audioLoopThread: Thread? = null
    private var discoveryReceiveThread: Thread? = null
    private var syncPingThread: Thread? = null

    val registeredSpeakers = ConcurrentHashMap<String, SpeakerDevice>()
    private val synthGenerator = SynthesizerAudioGenerator()
    val dspEngine = AudioDspEngine(MeshProtocol.SAMPLE_RATE)

    var activeAudioSource: AudioSourceType = AudioSourceType.SYSTEM_AUDIO
    var hostIpAddress: String = "127.0.0.1"
    var nowPlayingCustomTitle: String? = null
    var nowPlayingCustomArtist: String? = null
    var masterVolume: Float = 0.85f
    var isMasterMuted: Boolean = false
    var syncDelayOffsetMs: Int = 40
    var latencyMode: LatencyMode = LatencyMode.BALANCED
    var soundQuality: SoundQualityMode = SoundQualityMode.STUDIO_HIFI
    var audioProfile: AudioProfile = AudioProfile.ALL_STEREO
        set(value) {
            field = value
            dspEngine.currentProfile = value
            if (value == AudioProfile.SURROUND_5_1) {
                applySurround51Mapping()
            } else if (value == AudioProfile.ALL_STEREO) {
                applyAllStereoMapping()
            }
            broadcastConfigUpdate()
        }

    fun updateEqualizer(settings: EqualizerSettings) {
        dspEngine.updateEqualizer(settings)
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("SoundMeshMasterLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            multicastAddress = InetAddress.getByName(MeshProtocol.MULTICAST_GROUP)
            broadcastSocket = DatagramSocket().apply {
                broadcast = true
                try {
                    trafficClass = 0x10
                    sendBufferSize = 256 * 1024
                } catch (_: Exception) {}
            }

            multicastSocket = MulticastSocket(MeshProtocol.DISCOVERY_PORT).apply {
                reuseAddress = true
                try {
                    trafficClass = 0x10
                    receiveBufferSize = 256 * 1024
                    joinGroup(multicastAddress)
                } catch (e: Exception) {
                    Log.w("MeshBroadcaster", "Multicast join error: ${e.message}")
                }
            }

            startDiscoveryListener()
            startSyncPingLoop()
            startSyntheticAudioLoop()

            Log.i("MeshBroadcaster", "Mesh Broadcaster started with Low-Latency DSP Engine")
        } catch (e: Exception) {
            Log.e("MeshBroadcaster", "Failed to start broadcaster", e)
        }
    }

    fun setPlaying(playing: Boolean) {
        isPlaying.set(playing)
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get()

    fun feedCapturedPcm(pcmData: ByteArray, length: Int) {
        if (!isRunning.get() || !isPlaying.get()) return
        if (activeAudioSource != AudioSourceType.SYSTEM_AUDIO && activeAudioSource != AudioSourceType.PARTY_MIC) return
        broadcastPcmFrame(pcmData, length)
    }

    private fun startSyntheticAudioLoop() {
        audioLoopThread = Thread({
            val pcmBuffer = ByteArray(MeshProtocol.AUDIO_PAYLOAD_SIZE)
            val frameDurationMs = (MeshProtocol.SAMPLES_PER_FRAME * 1000L) / MeshProtocol.SAMPLE_RATE

            while (isRunning.get()) {
                val loopStartTime = System.currentTimeMillis()

                if (isPlaying.get() && activeAudioSource == AudioSourceType.PARTY_BEATS) {
                    synthGenerator.generatePcmFrame(pcmBuffer, 0, pcmBuffer.size)

                    var sumSquares = 0.0
                    val numSamples = pcmBuffer.size / 2
                    val byteBuffer = ByteBuffer.wrap(pcmBuffer).order(ByteOrder.LITTLE_ENDIAN)
                    val bandEnergies = FloatArray(8) { 0f }

                    for (i in 0 until numSamples) {
                        val sample = byteBuffer.short.toDouble()
                        sumSquares += sample * sample
                        val band = i % 8
                        bandEnergies[band] += abs(sample.toFloat()) / 32768f
                    }
                    val rms = (sqrt(sumSquares / numSamples) / 32768.0).toFloat().coerceIn(0f, 1f)
                    val bands = bandEnergies.map { (it / (numSamples / 8f)).coerceIn(0.05f, 1f) }
                    onAudioLevelUpdated(rms, bands)

                    broadcastPcmFrame(pcmBuffer, pcmBuffer.size)
                }

                val elapsed = System.currentTimeMillis() - loopStartTime
                val sleepTime = frameDurationMs - elapsed
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }, "SoundMesh-MasterAudioLoop").apply { start() }
    }

    private fun broadcastPcmFrame(pcmData: ByteArray, length: Int) {
        val socket = broadcastSocket ?: return
        val seq = sequenceNumber.incrementAndGet()
        val presentationTimestampNanos = System.nanoTime() + (syncDelayOffsetMs * 1_000_000L)

        val processedPcm = ByteArray(length)
        val effectiveMasterVol = if (isMasterMuted) 0f else masterVolume
        dspEngine.processStereoPcm(
            input = pcmData,
            length = length,
            output = processedPcm,
            targetChannel = SpeakerChannel.STEREO_ALL,
            masterGain = effectiveMasterVol
        )

        val packetBytes = MeshProtocol.createAudioPacket(seq, presentationTimestampNanos, processedPcm, 0, length)

        // Audio is multicast once. Sending the same PCM frame by multicast AND unicast
        // causes every receiver to enqueue two copies, which sounds like an echo/doubling.
        try {
            multicastAddress?.let { mAddr ->
                val packet = DatagramPacket(packetBytes, packetBytes.size, mAddr, MeshProtocol.AUDIO_PORT)
                socket.send(packet)
            }
        } catch (e: Exception) {
            Log.v("MeshBroadcaster", "Multicast packet error: ${e.message}")
        }
    }

    private fun startDiscoveryListener() {
        discoveryReceiveThread = Thread({
            val receiveBuffer = ByteArray(MeshProtocol.MAX_PACKET_SIZE)
            while (isRunning.get()) {
                try {
                    val socket = multicastSocket ?: break
                    val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(packet)

                    val length = packet.length
                    if (length >= MeshProtocol.HEADER_SIZE) {
                        val byteBuf = ByteBuffer.wrap(receiveBuffer, 0, length).order(ByteOrder.BIG_ENDIAN)
                        val magic1 = byteBuf.get()
                        val magic2 = byteBuf.get()
                        if (magic1 == MeshProtocol.MAGIC_BYTE_1 && magic2 == MeshProtocol.MAGIC_BYTE_2) {
                            val type = byteBuf.get()
                            byteBuf.get()
                            byteBuf.getLong()
                            byteBuf.getLong()
                            val payloadLen = byteBuf.getShort().toInt()
                            if (payloadLen < 0 || payloadLen > byteBuf.remaining()) continue

                            val payloadBytes = ByteArray(payloadLen)
                            byteBuf.get(payloadBytes)

                            when (type) {
                                MeshProtocol.TYPE_ANNOUNCE_SPEAKER -> {
                                    val info = String(payloadBytes, Charsets.UTF_8).split("|")
                                    val id = info.getOrNull(0) ?: packet.address.hostAddress ?: "speaker"
                                    val name = info.getOrNull(1) ?: "Satellite Speaker"
                                    val battery = info.getOrNull(2)?.toIntOrNull() ?: 85
                                    val isCharging = info.getOrNull(3)?.toBooleanStrictOrNull() ?: false
                                    val wifiDbm = info.getOrNull(4)?.toIntOrNull() ?: -52
                                    val wifiLevel = info.getOrNull(5)?.toIntOrNull() ?: 4
                                    val volumePercent = info.getOrNull(6)?.toIntOrNull() ?: 90
                                    val isMuted = info.getOrNull(7)?.toBooleanStrictOrNull() ?: false
                                    val channelStr = info.getOrNull(8)
                                    val deviceModel = info.getOrNull(9) ?: "Satellite Phone"
                                    val ip = packet.address.hostAddress ?: "127.0.0.1"

                                    val existing = registeredSpeakers[id]
                                    val channel = existing?.channel ?: channelStr?.let {
                                        try { SpeakerChannel.valueOf(it) } catch (_: Exception) { null }
                                    } ?: assignAutoChannel(registeredSpeakers.size)

                                    val speaker = existing?.copy(
                                        name = if (existing.name.isNotBlank()) existing.name else name,
                                        ipAddress = ip,
                                        batteryPercent = battery,
                                        isCharging = isCharging,
                                        wifiSignalDbm = wifiDbm,
                                        wifiSignalLevel = wifiLevel,
                                        volume = (volumePercent / 100f).coerceIn(0f, 1f),
                                        isMuted = isMuted,
                                        deviceModel = deviceModel,
                                        isConnected = true,
                                        lastSeenTimestamp = System.currentTimeMillis()
                                    ) ?: SpeakerDevice(
                                        id = id,
                                        name = name,
                                        ipAddress = ip,
                                        port = MeshProtocol.AUDIO_PORT,
                                        batteryPercent = battery,
                                        isCharging = isCharging,
                                        wifiSignalDbm = wifiDbm,
                                        wifiSignalLevel = wifiLevel,
                                        volume = (volumePercent / 100f).coerceIn(0f, 1f),
                                        isMuted = isMuted,
                                        deviceModel = deviceModel,
                                        channel = channel
                                    )

                                    if (registeredSpeakers.size < 10 || registeredSpeakers.containsKey(id)) {
                                        registeredSpeakers[id] = speaker
                                        onSpeakerDiscovered(speaker)
                                    }
                                }

                                MeshProtocol.TYPE_SYNC_PONG -> {
                                    val now = System.nanoTime()
                                    val speakerId = String(payloadBytes, Charsets.UTF_8)
                                    val pingSentNanos = pendingSyncPings.remove(speakerId)
                                    if (pingSentNanos != null) {
                                        val rttNanos = (now - pingSentNanos).coerceAtLeast(0L)
                                        val latencyMs = (rttNanos / 2_000_000L).coerceAtLeast(1L)
                                        registeredSpeakers[speakerId]?.let { spk ->
                                            registeredSpeakers[speakerId] = spk.copy(latencyMs = latencyMs)
                                            onSpeakerLatencyUpdated(speakerId, latencyMs)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.v("MeshBroadcaster", "Discovery loop exception: ${e.message}")
                }
            }
        }, "SoundMesh-DiscoveryListener").apply { start() }
    }

    private fun startSyncPingLoop() {
        syncPingThread = Thread({
            while (isRunning.get()) {
                try {
                    registeredSpeakers.values.forEach { speaker ->
                        try {
                            val pingTimestamp = System.nanoTime()
                            val pingBuffer = ByteBuffer.allocate(MeshProtocol.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
                            pingBuffer.put(MeshProtocol.MAGIC_BYTE_1)
                            pingBuffer.put(MeshProtocol.MAGIC_BYTE_2)
                            pingBuffer.put(MeshProtocol.TYPE_SYNC_PING)
                            pingBuffer.put(0.toByte())
                            pingBuffer.putLong(0L)
                            pingBuffer.putLong(pingTimestamp)
                            pingBuffer.putShort(0.toShort())
                            val ip = InetAddress.getByName(speaker.ipAddress)
                            val packet = DatagramPacket(pingBuffer.array(), pingBuffer.array().size, ip, speaker.port)
                            pendingSyncPings[speaker.id] = pingTimestamp
                            broadcastSocket?.send(packet)
                        } catch (_: Exception) {}
                    }

                    val beaconBytes = MeshProtocol.createBeaconPacket("Master Controller", hostIpAddress, isPlaying.get())
                    multicastAddress?.let { mAddr ->
                        val packet = DatagramPacket(beaconBytes, beaconBytes.size, mAddr, MeshProtocol.DISCOVERY_PORT)
                        broadcastSocket?.send(packet)
                    }

                    val telemetry = SystemStatsProvider.getTelemetry(context)
                    val nowTitle = nowPlayingCustomTitle ?: when (activeAudioSource) {
                        AudioSourceType.SYSTEM_AUDIO -> "Spotify / System Audio Stream"
                        AudioSourceType.PARTY_BEATS -> "DSP Algorithmic Party Beats"
                        AudioSourceType.PARTY_MIC -> "Live Wireless Microphone"
                    }
                    val nowArtist = nowPlayingCustomArtist ?: when (activeAudioSource) {
                        AudioSourceType.SYSTEM_AUDIO -> "44.1 kHz PCM • System Capture"
                        AudioSourceType.PARTY_BEATS -> "Synthesizer • ${audioProfile.title}"
                        AudioSourceType.PARTY_MIC -> "Direct Stream • ${latencyMode.title}"
                    }

                    val masterStatsBytes = MeshProtocol.createMasterStatsPacket(
                        deviceName = "Master Host Phone",
                        deviceModel = telemetry.deviceModel,
                        ipAddress = hostIpAddress,
                        batteryPercent = telemetry.batteryPercent,
                        isCharging = telemetry.isCharging,
                        wifiSignalDbm = telemetry.wifiSignalDbm,
                        wifiSignalLevel = telemetry.wifiSignalLevel,
                        wifiSsid = telemetry.wifiSsid,
                        wifiLinkSpeedMbps = telemetry.wifiLinkSpeedMbps,
                        masterVolumePercent = (masterVolume * 100).toInt(),
                        isMuted = isMasterMuted,
                        nowPlayingTitle = nowTitle,
                        nowPlayingArtist = nowArtist,
                        isPlaying = isPlaying.get(),
                        audioProfile = audioProfile.name,
                        latencyMode = latencyMode.name,
                        activeBitrateKbps = soundQuality.bitrateKbps,
                        connectedSpeakersCount = registeredSpeakers.size
                    )

                    multicastAddress?.let { mAddr ->
                        val packet = DatagramPacket(masterStatsBytes, masterStatsBytes.size, mAddr, MeshProtocol.DISCOVERY_PORT)
                        broadcastSocket?.send(packet)
                    }

                    registeredSpeakers.values.forEach { speaker ->
                        try {
                            val ip = InetAddress.getByName(speaker.ipAddress)
                            val packet = DatagramPacket(masterStatsBytes, masterStatsBytes.size, ip, speaker.port)
                            broadcastSocket?.send(packet)
                        } catch (_: Exception) {}
                    }

                    Thread.sleep(1500)
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.v("MeshBroadcaster", "Ping loop: ${e.message}")
                }
            }
        }, "SoundMesh-SyncPing").apply { start() }
    }

    fun calibrateAndAlignAllSpeakers(): Int {
        val speakers = registeredSpeakers.values.toList()
        if (speakers.isEmpty()) return syncDelayOffsetMs

        val maxRttLatency = speakers.maxOfOrNull { it.latencyMs } ?: 18L
        val headroom = when (latencyMode) {
            LatencyMode.ULTRA_LOW -> 12
            LatencyMode.BALANCED -> 24
            LatencyMode.STABLE -> 50
        }
        val calculatedDelay = (maxRttLatency.toInt() + headroom).coerceIn(16, 250)
        syncDelayOffsetMs = calculatedDelay

        val targetPlayNanoTime = System.nanoTime() + (syncDelayOffsetMs * 1_000_000L)
        val alignPacketBytes = MeshProtocol.createAutoSyncPacket(targetPlayNanoTime, syncDelayOffsetMs)

        Thread({
            try {
                multicastAddress?.let { mAddr ->
                    val packet = DatagramPacket(alignPacketBytes, alignPacketBytes.size, mAddr, MeshProtocol.AUDIO_PORT)
                    broadcastSocket?.send(packet)
                }
                speakers.forEach { speaker ->
                    try {
                        val ip = InetAddress.getByName(speaker.ipAddress)
                        val packet = DatagramPacket(alignPacketBytes, alignPacketBytes.size, ip, speaker.port)
                        broadcastSocket?.send(packet)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("MeshBroadcaster", "Auto sync align broadcast error: ${e.message}")
            }
        }).start()

        return syncDelayOffsetMs
    }

    fun applySurround51Mapping() {
        val speakers = registeredSpeakers.values.toList()
        speakers.forEachIndexed { index, speaker ->
            val assignedChannel = when (index) {
                0 -> SpeakerChannel.CENTER
                1 -> SpeakerChannel.LEFT_ONLY
                2 -> SpeakerChannel.RIGHT_ONLY
                3 -> SpeakerChannel.SURROUND_LEFT
                4 -> SpeakerChannel.SURROUND_RIGHT
                5 -> SpeakerChannel.SUBWOOFER
                else -> SpeakerChannel.STEREO_ALL
            }
            updateSpeakerChannel(speaker.id, assignedChannel)
        }
    }

    fun applyAllStereoMapping() {
        registeredSpeakers.values.forEach { speaker ->
            updateSpeakerChannel(speaker.id, SpeakerChannel.STEREO_ALL)
        }
    }

    fun broadcastConfigUpdate() {
        val configPacketBytes = MeshProtocol.createConfigPacket(latencyMode.name, audioProfile.name)
        Thread({
            try {
                multicastAddress?.let { mAddr ->
                    val packet = DatagramPacket(configPacketBytes, configPacketBytes.size, mAddr, MeshProtocol.DISCOVERY_PORT)
                    broadcastSocket?.send(packet)
                }
                registeredSpeakers.values.forEach { speaker ->
                    try {
                        val ip = InetAddress.getByName(speaker.ipAddress)
                        val packet = DatagramPacket(configPacketBytes, configPacketBytes.size, ip, speaker.port)
                        broadcastSocket?.send(packet)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }).start()
    }

    private fun assignAutoChannel(index: Int): SpeakerChannel {
        return when (index % 4) {
            0 -> SpeakerChannel.STEREO_ALL
            1 -> SpeakerChannel.LEFT_ONLY
            2 -> SpeakerChannel.RIGHT_ONLY
            else -> SpeakerChannel.CENTER
        }
    }

    fun updateSpeakerChannel(speakerId: String, channel: SpeakerChannel) {
        registeredSpeakers[speakerId]?.let {
            registeredSpeakers[speakerId] = it.copy(channel = channel)
        }
    }

    fun updateSpeakerVolume(speakerId: String, volume: Float, isMuted: Boolean) {
        registeredSpeakers[speakerId]?.let {
            registeredSpeakers[speakerId] = it.copy(volume = volume, isMuted = isMuted)
        }
    }

    fun sendSpeakerTuningUpdate(speaker: SpeakerDevice) {
        registeredSpeakers[speaker.id] = speaker
        val packetBytes = MeshProtocol.createSpeakerTuningPacket(
            speakerId = speaker.id,
            channel = speaker.channel.name,
            zoneId = speaker.zone.name,
            audioProfile = speaker.audioProfile.name,
            eqEnabled = speaker.equalizer.isEnabled,
            eqPreset = speaker.equalizer.preset.name,
            band60 = speaker.equalizer.band60Hz,
            band250 = speaker.equalizer.band250Hz,
            band1k = speaker.equalizer.band1kHz,
            band4k = speaker.equalizer.band4kHz,
            band12k = speaker.equalizer.band12kHz,
            fineTrimMs = speaker.fineTrimMs,
            volume = speaker.volume,
            isMuted = speaker.isMuted
        )
        Thread({
            try {
                val ip = InetAddress.getByName(speaker.ipAddress)
                val directPacket = DatagramPacket(packetBytes, packetBytes.size, ip, speaker.port)
                broadcastSocket?.send(directPacket)
                val discPacket = DatagramPacket(packetBytes, packetBytes.size, ip, MeshProtocol.DISCOVERY_PORT)
                broadcastSocket?.send(discPacket)
                multicastAddress?.let { mAddr ->
                    val mPacket = DatagramPacket(packetBytes, packetBytes.size, mAddr, MeshProtocol.DISCOVERY_PORT)
                    broadcastSocket?.send(mPacket)
                }
            } catch (e: Exception) {
                Log.w("MeshBroadcaster", "Tuning update error: ${e.message}")
            }
        }).start()
    }

    fun removeSpeaker(speakerId: String) {
        registeredSpeakers.remove(speakerId)
        pendingSyncPings.remove(speakerId)
    }

    fun sendChirp(speakerId: String) {
        registeredSpeakers[speakerId]?.let { speaker ->
            Thread({
                try {
                    val chirpBuffer = ByteBuffer.allocate(MeshProtocol.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
                    chirpBuffer.put(MeshProtocol.MAGIC_BYTE_1)
                    chirpBuffer.put(MeshProtocol.MAGIC_BYTE_2)
                    chirpBuffer.put(MeshProtocol.TYPE_IDENTIFY_CHIRP)
                    chirpBuffer.put(0.toByte())
                    chirpBuffer.putLong(0L)
                    chirpBuffer.putLong(System.nanoTime())
                    chirpBuffer.putShort(0.toShort())
                    val chirpBytes = chirpBuffer.array()
                    val ip = InetAddress.getByName(speaker.ipAddress)
                    val packet = DatagramPacket(chirpBytes, chirpBytes.size, ip, speaker.port)
                    broadcastSocket?.send(packet)
                } catch (e: Exception) {
                    Log.w("MeshBroadcaster", "Chirp error: ${e.message}")
                }
            }).start()
        }
    }

    fun stop() {
        isRunning.set(false)
        isPlaying.set(false)
        pendingSyncPings.clear()

        try {
            audioLoopThread?.interrupt()
            discoveryReceiveThread?.interrupt()
            syncPingThread?.interrupt()
            broadcastSocket?.close()
            multicastSocket?.close()
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.e("MeshBroadcaster", "Error stopping broadcaster", e)
        }
    }
}
