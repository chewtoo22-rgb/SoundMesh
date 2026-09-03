package com.example.audio.mesh

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.audio.dsp.AudioDspEngine
import com.example.model.AudioProfile
import com.example.model.EqualizerPreset
import com.example.model.EqualizerSettings
import com.example.model.LatencyMode
import com.example.model.MasterSystemStats
import com.example.model.SpeakerChannel
import com.example.model.SpatialZone
import com.example.util.SystemStatsProvider
import java.net.DatagramPacket
import java.net.MulticastSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

data class ScheduledAudioChunk(
    val seq: Long,
    val presentationTimeNanos: Long,
    val audioBytes: ByteArray
) : Comparable<ScheduledAudioChunk> {
    override fun compareTo(other: ScheduledAudioChunk): Int = presentationTimeNanos.compareTo(other.presentationTimeNanos)
}

class MeshAudioReceiver(
    private val context: Context,
    private val onConnectedToMaster: (String) -> Unit,
    private val onAudioLevelUpdated: (Float, List<Float>) -> Unit,
    private val onChirpReceived: () -> Unit,
    private val onMasterStatsReceived: (MasterSystemStats) -> Unit = {},
    private val onSpeakerTuningReceived: (SpatialZone, AudioProfile, EqualizerSettings, SpeakerChannel, Int, Float, Boolean) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val speakerId = UUID.randomUUID().toString().take(8)
    val speakerName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val isRunning = AtomicBoolean(false)
    private val lastAudioSequence = AtomicLong(-1L)
    private var multicastLock: WifiManager.MulticastLock? = null
    private var audioSocket: MulticastSocket? = null
    private var audioTrack: AudioTrack? = null

    private var audioReceiveThread: Thread? = null
    private var announceThread: Thread? = null
    private var scheduledPlayoutThread: Thread? = null

    // Timestamp-ordered jitter buffer. Keeping this bounded prevents a bad network
    // condition from turning latency into an ever-growing audio delay.
    private val playoutQueue = PriorityBlockingQueue<ScheduledAudioChunk>(48)

    val dspEngine = AudioDspEngine(MeshProtocol.SAMPLE_RATE)

    @Volatile
    private var masterClockOffsetNanos: Long = 0L

    var currentChannel: SpeakerChannel = SpeakerChannel.STEREO_ALL
    var localVolume: Float = 0.90f
    var localLatencyTrimMs: Int = 0
    var masterIpAddress: String? = null

    fun updateEqualizer(settings: EqualizerSettings) {
        dspEngine.updateEqualizer(settings)
    }

    fun setAudioProfile(profile: AudioProfile) {
        dspEngine.currentProfile = profile
    }

    fun start(targetMasterIp: String? = null) {
        if (isRunning.getAndSet(true)) return
        masterIpAddress = targetMasterIp

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("SoundMeshReceiverLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                MeshProtocol.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, MeshProtocol.AUDIO_PAYLOAD_SIZE * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(MeshProtocol.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack?.play()

            val mAddress = InetAddress.getByName(MeshProtocol.MULTICAST_GROUP)
            // One socket owns AUDIO_PORT. A second DatagramSocket on the same port is
            // not portable on Android and can make the receiver fail during startup.
            // MulticastSocket is also capable of receiving the master's unicast control
            // packets because they target the same bound UDP port.
            audioSocket = MulticastSocket(MeshProtocol.AUDIO_PORT).apply {
                reuseAddress = true
                try {
                    trafficClass = 0x10
                    receiveBufferSize = 256 * 1024
                    joinGroup(mAddress)
                } catch (e: Exception) {
                    Log.w("MeshReceiver", "Join multicast error: ${e.message}")
                }
            }

            startMulticastReceiveThread()
            startScheduledPlayoutThread()
            startAnnounceLoop()

            Log.i("MeshReceiver", "Mesh Receiver started as $speakerName ($speakerId) with synchronized playout")
        } catch (e: Exception) {
            Log.e("MeshReceiver", "Failed to start receiver", e)
            stop()
        }
    }

    private fun startScheduledPlayoutThread() {
        scheduledPlayoutThread = Thread({
            while (isRunning.get()) {
                try {
                    val chunk = playoutQueue.poll(20, TimeUnit.MILLISECONDS) ?: continue
                    val targetLocalNanos = chunk.presentationTimeNanos - masterClockOffsetNanos + (localLatencyTrimMs * 1_000_000L)

                    var leadTimeNanos = targetLocalNanos - System.nanoTime()
                    if (leadTimeNanos > 0L) {
                        // Sleep in short slices so stop() remains responsive and the
                        // scheduler does not oversleep across a packet deadline.
                        while (leadTimeNanos > 0L && isRunning.get()) {
                            val sliceNanos = leadTimeNanos.coerceAtMost(5_000_000L)
                            try {
                                TimeUnit.NANOSECONDS.sleep(sliceNanos)
                            } catch (_: InterruptedException) {
                                return@Thread
                            }
                            leadTimeNanos = targetLocalNanos - System.nanoTime()
                        }
                    } else if (leadTimeNanos < -70_000_000L) {
                        // Too late to preserve sync. Dropping a frame is preferable to
                        // injecting an audible delayed echo.
                        continue
                    }

                    playAudioWithChannelProcessing(chunk.audioBytes)
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.v("MeshReceiver", "Playout error: ${e.message}")
                }
            }
        }, "SoundMesh-ScheduledPlayout").apply { start() }
    }

    private fun startAnnounceLoop() {
        announceThread = Thread({
            while (isRunning.get()) {
                try {
                    val telemetry = SystemStatsProvider.getTelemetry(context)
                    val announceBytes = MeshProtocol.createAnnouncePacket(
                        speakerId = speakerId,
                        speakerName = speakerName,
                        battery = telemetry.batteryPercent,
                        isCharging = telemetry.isCharging,
                        wifiSignalDbm = telemetry.wifiSignalDbm,
                        wifiSignalLevel = telemetry.wifiSignalLevel,
                        volumePercent = (localVolume * 100).toInt(),
                        isMuted = false,
                        channel = currentChannel.name,
                        deviceModel = telemetry.deviceModel
                    )

                    masterIpAddress?.let { ipStr ->
                        try {
                            val ip = InetAddress.getByName(ipStr)
                            val packet = DatagramPacket(announceBytes, announceBytes.size, ip, MeshProtocol.DISCOVERY_PORT)
                            audioSocket?.send(packet)
                        } catch (_: Exception) {}
                    }

                    try {
                        val mAddress = InetAddress.getByName(MeshProtocol.MULTICAST_GROUP)
                        val packet = DatagramPacket(announceBytes, announceBytes.size, mAddress, MeshProtocol.DISCOVERY_PORT)
                        audioSocket?.send(packet)
                    } catch (_: Exception) {}

                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.v("MeshReceiver", "Announce error: ${e.message}")
                }
            }
        }, "SoundMesh-AnnounceLoop").apply { start() }
    }

    private fun startMulticastReceiveThread() {
        audioReceiveThread = Thread({
            val buffer = ByteArray(MeshProtocol.MAX_PACKET_SIZE)
            while (isRunning.get()) {
                try {
                    val socket = audioSocket ?: break
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    processIncomingPacket(packet)
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.v("MeshReceiver", "Receive loop error: ${e.message}")
                }
            }
        }, "SoundMesh-AudioReceiver").apply { start() }
    }

    private fun processIncomingPacket(packet: DatagramPacket) {
        val length = packet.length
        if (length < MeshProtocol.HEADER_SIZE) return

        val byteBuf = ByteBuffer.wrap(packet.data, packet.offset, length).order(ByteOrder.BIG_ENDIAN)
        if (byteBuf.get() != MeshProtocol.MAGIC_BYTE_1 || byteBuf.get() != MeshProtocol.MAGIC_BYTE_2) return

        val type = byteBuf.get()
        byteBuf.get()
        val seq = byteBuf.getLong()
        val timestamp = byteBuf.getLong()
        val payloadLen = byteBuf.getShort().toInt()
        if (payloadLen < 0 || payloadLen > byteBuf.remaining()) return

        val senderIp = packet.address.hostAddress
        if (senderIp != null && masterIpAddress != senderIp) {
            masterIpAddress = senderIp
            onConnectedToMaster(senderIp)
        }

        when (type) {
            MeshProtocol.TYPE_AUDIO_DATA -> {
                // The master uses multicast for the audio stream. Reject duplicate or
                // reordered sequence numbers so an accidental retransmit cannot create
                // an audible doubled frame.
                if (!acceptAudioSequence(seq)) return
                val audioBytes = ByteArray(payloadLen)
                byteBuf.get(audioBytes)
                if (playoutQueue.size >= 40) {
                    // Prefer dropping the oldest queued frame when the network gets ahead.
                    playoutQueue.poll()
                }
                playoutQueue.offer(ScheduledAudioChunk(seq, timestamp, audioBytes))
            }

            MeshProtocol.TYPE_SYNC_PING -> {
                val localReceiveNanos = System.nanoTime()
                // This is an offset estimate, not a claimed exact clock measurement.
                // Presentation timestamps are intentionally buffered to absorb one-way jitter.
                masterClockOffsetNanos = timestamp - localReceiveNanos

                try {
                    val pongPayload = speakerId.toByteArray(Charsets.UTF_8)
                    val pongBuf = ByteBuffer.allocate(MeshProtocol.HEADER_SIZE + pongPayload.size).order(ByteOrder.BIG_ENDIAN)
                    pongBuf.put(MeshProtocol.MAGIC_BYTE_1)
                    pongBuf.put(MeshProtocol.MAGIC_BYTE_2)
                    pongBuf.put(MeshProtocol.TYPE_SYNC_PONG)
                    pongBuf.put(0.toByte())
                    pongBuf.putLong(seq)
                    pongBuf.putLong(timestamp)
                    pongBuf.putShort(pongPayload.size.toShort())
                    pongBuf.put(pongPayload)
                    audioSocket?.send(DatagramPacket(pongBuf.array(), pongBuf.array().size, packet.address, MeshProtocol.DISCOVERY_PORT))
                } catch (_: Exception) {}
            }

            MeshProtocol.TYPE_AUTO_SYNC_ALIGN -> {
                // timestamp is the master's future presentation anchor.
                val localNowNanos = System.nanoTime()
                masterClockOffsetNanos = timestamp - localNowNanos
                playoutQueue.clear()
                lastAudioSequence.set(-1L)
                Log.i("MeshReceiver", "Received AUTO_SYNC_ALIGN. Clock anchor updated by ${masterClockOffsetNanos / 1_000_000}ms")
            }

            MeshProtocol.TYPE_CONFIG_UPDATE -> {
                val payloadBytes = ByteArray(payloadLen)
                byteBuf.get(payloadBytes)
                val parts = String(payloadBytes, Charsets.UTF_8).split("|")
                if (parts.size >= 2) {
                    try { setAudioProfile(AudioProfile.valueOf(parts[1])) } catch (_: Exception) {}
                }
            }

            MeshProtocol.TYPE_IDENTIFY_CHIRP -> {
                playIdentifyChirp()
                onChirpReceived()
            }

            MeshProtocol.TYPE_SPEAKER_TUNING_UPDATE -> {
                try {
                    val payloadBytes = ByteArray(payloadLen)
                    byteBuf.get(payloadBytes)
                    val info = String(payloadBytes, Charsets.UTF_8).split("|")
                    val targetSpeakerId = info.getOrNull(0) ?: ""
                    if (targetSpeakerId.isEmpty() || targetSpeakerId == speakerId) {
                        val channelStr = info.getOrNull(1) ?: "STEREO_ALL"
                        val zoneStr = info.getOrNull(2) ?: "ALL_PARTY"
                        val profileStr = info.getOrNull(3) ?: "ALL_STEREO"
                        val eqEnabled = info.getOrNull(4)?.toBooleanStrictOrNull() ?: true
                        val eqPresetStr = info.getOrNull(5) ?: "FLAT"
                        val b60 = info.getOrNull(6)?.toFloatOrNull() ?: 0f
                        val b250 = info.getOrNull(7)?.toFloatOrNull() ?: 0f
                        val b1k = info.getOrNull(8)?.toFloatOrNull() ?: 0f
                        val b4k = info.getOrNull(9)?.toFloatOrNull() ?: 0f
                        val b12k = info.getOrNull(10)?.toFloatOrNull() ?: 0f
                        val trimMs = info.getOrNull(11)?.toIntOrNull() ?: 0
                        val vol = info.getOrNull(12)?.toFloatOrNull() ?: localVolume
                        val mute = info.getOrNull(13)?.toBooleanStrictOrNull() ?: false

                        val ch = try { SpeakerChannel.valueOf(channelStr) } catch (_: Exception) { SpeakerChannel.STEREO_ALL }
                        val zone = try { SpatialZone.valueOf(zoneStr) } catch (_: Exception) { SpatialZone.ALL_PARTY }
                        val prof = try { AudioProfile.valueOf(profileStr) } catch (_: Exception) { AudioProfile.ALL_STEREO }
                        val preset = try { EqualizerPreset.valueOf(eqPresetStr) } catch (_: Exception) { EqualizerPreset.CUSTOM }
                        val eq = EqualizerSettings(
                            preset = preset,
                            band60Hz = b60,
                            band250Hz = b250,
                            band1kHz = b1k,
                            band4kHz = b4k,
                            band12kHz = b12k,
                            isEnabled = eqEnabled
                        )

                        currentChannel = ch
                        localLatencyTrimMs = trimMs
                        localVolume = if (mute) 0f else vol
                        setAudioProfile(prof)
                        updateEqualizer(eq)
                        onSpeakerTuningReceived(zone, prof, eq, ch, trimMs, vol, mute)
                    }
                } catch (e: Exception) {
                    Log.w("MeshReceiver", "Error parsing speaker tuning update: ${e.message}")
                }
            }

            MeshProtocol.TYPE_MASTER_STATS -> {
                try {
                    val payloadBytes = ByteArray(payloadLen)
                    byteBuf.get(payloadBytes)
                    val info = String(payloadBytes, Charsets.UTF_8).split("|")
                    val deviceName = info.getOrNull(0) ?: "Master Host Phone"
                    val deviceModel = info.getOrNull(1) ?: "Samsung Galaxy"
                    val ipAddress = info.getOrNull(2) ?: (senderIp ?: "127.0.0.1")
                    val batteryPercent = info.getOrNull(3)?.toIntOrNull() ?: 88
                    val isCharging = info.getOrNull(4)?.toBooleanStrictOrNull() ?: false
                    val wifiDbm = info.getOrNull(5)?.toIntOrNull() ?: -50
                    val wifiLevel = info.getOrNull(6)?.toIntOrNull() ?: 4
                    val wifiSsid = info.getOrNull(7) ?: "SoundMesh Wi-Fi"
                    val wifiSpeed = info.getOrNull(8)?.toIntOrNull() ?: 433
                    val volumePercent = info.getOrNull(9)?.toIntOrNull() ?: 85
                    val isMuted = info.getOrNull(10)?.toBooleanStrictOrNull() ?: false
                    val nowPlayingTitle = info.getOrNull(11) ?: "Spotify / System Audio"
                    val nowPlayingArtist = info.getOrNull(12) ?: "All Apps Audio Capture (44.1 kHz PCM)"
                    val isPlaying = info.getOrNull(13)?.toBooleanStrictOrNull() ?: false
                    val profile = try { AudioProfile.valueOf(info.getOrNull(14) ?: "ALL_STEREO") } catch (_: Exception) { AudioProfile.ALL_STEREO }
                    val latency = try { LatencyMode.valueOf(info.getOrNull(15) ?: "BALANCED") } catch (_: Exception) { LatencyMode.BALANCED }
                    val bitrate = info.getOrNull(16)?.toIntOrNull() ?: 1411
                    val connectedCount = info.getOrNull(17)?.toIntOrNull() ?: 0

                    onMasterStatsReceived(
                        MasterSystemStats(
                            deviceName = deviceName,
                            deviceModel = deviceModel,
                            ipAddress = ipAddress,
                            batteryPercent = batteryPercent,
                            isCharging = isCharging,
                            wifiSignalDbm = wifiDbm,
                            wifiSignalLevel = wifiLevel,
                            wifiSsid = wifiSsid,
                            wifiLinkSpeedMbps = wifiSpeed,
                            masterVolumePercent = volumePercent,
                            isMuted = isMuted,
                            nowPlayingTitle = nowPlayingTitle,
                            nowPlayingArtist = nowPlayingArtist,
                            isPlaying = isPlaying,
                            audioProfile = profile,
                            latencyMode = latency,
                            activeBitrateKbps = bitrate,
                            connectedSpeakersCount = connectedCount,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.v("MeshReceiver", "Master stats parsing error: ${e.message}")
                }
            }

            MeshProtocol.TYPE_MASTER_BEACON -> {
                try {
                    val payloadBytes = ByteArray(payloadLen)
                    byteBuf.get(payloadBytes)
                    val info = String(payloadBytes, Charsets.UTF_8).split("|")
                    val hostIp = info.getOrNull(1) ?: senderIp
                    if (hostIp != null && hostIp != "host" && masterIpAddress != hostIp) {
                        masterIpAddress = hostIp
                        onConnectedToMaster(hostIp)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun acceptAudioSequence(sequence: Long): Boolean {
        while (true) {
            val previous = lastAudioSequence.get()
            if (sequence <= previous) return false
            if (lastAudioSequence.compareAndSet(previous, sequence)) return true
        }
    }

    private fun playAudioWithChannelProcessing(audioBytes: ByteArray) {
        val track = audioTrack ?: return
        val stereoFrames = audioBytes.size / 4
        if (stereoFrames <= 0) return

        val processed = ByteArray(audioBytes.size)
        dspEngine.processStereoPcm(
            input = audioBytes,
            length = audioBytes.size,
            output = processed,
            targetChannel = currentChannel,
            masterGain = localVolume
        )
        track.write(processed, 0, processed.size, AudioTrack.WRITE_BLOCKING)

        val inBuf = ByteBuffer.wrap(processed).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        val bandEnergies = FloatArray(8) { 0f }
        for (i in 0 until stereoFrames) {
            val left = inBuf.short
            val right = inBuf.short
            sumSquares += (left * left + right * right) / 2.0
            bandEnergies[i % 8] += (abs(left.toInt()) + abs(right.toInt())) / 65536f
        }

        val rms = (sqrt(sumSquares / stereoFrames) / 32768.0).toFloat().coerceIn(0f, 1f)
        val bands = bandEnergies.map { (it / (stereoFrames / 8f)).coerceIn(0.05f, 1f) }
        onAudioLevelUpdated(rms, bands)
    }

    private fun playIdentifyChirp() {
        Thread({
            val track = audioTrack ?: return@Thread
            val chirpSampleCount = MeshProtocol.SAMPLE_RATE / 4
            val buffer = ByteArray(chirpSampleCount * 4)
            val byteBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until chirpSampleCount) {
                val freq = 1200.0 + (i.toDouble() / chirpSampleCount) * 1200.0
                val sample = (sin(2.0 * Math.PI * freq * (i.toDouble() / MeshProtocol.SAMPLE_RATE)) * 28000.0).toInt().toShort()
                byteBuf.putShort(sample)
                byteBuf.putShort(sample)
            }
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }).start()
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 88
        } catch (_: Exception) {
            88
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            audioReceiveThread?.interrupt()
            announceThread?.interrupt()
            scheduledPlayoutThread?.interrupt()
            playoutQueue.clear()
            audioSocket?.close()
            audioSocket = null

            audioTrack?.let { track ->
                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                } catch (_: Exception) {}
                track.release()
            }
            audioTrack = null

            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.e("MeshReceiver", "Error stopping receiver", e)
        }
    }
}
