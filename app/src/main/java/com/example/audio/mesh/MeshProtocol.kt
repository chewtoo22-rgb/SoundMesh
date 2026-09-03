package com.example.audio.mesh

import java.nio.ByteBuffer
import java.nio.ByteOrder

object MeshProtocol {
    const val MULTICAST_GROUP = "239.255.42.99"
    const val AUDIO_PORT = 9876
    const val DISCOVERY_PORT = 9877

    const val MAGIC_BYTE_1: Byte = 0x53.toByte() // 'S'
    const val MAGIC_BYTE_2: Byte = 0x4D.toByte() // 'M'

    const val TYPE_AUDIO_DATA: Byte = 0x01
    const val TYPE_SYNC_PING: Byte = 0x02
    const val TYPE_SYNC_PONG: Byte = 0x03
    const val TYPE_ANNOUNCE_SPEAKER: Byte = 0x04
    const val TYPE_MASTER_BEACON: Byte = 0x05
    const val TYPE_COMMAND_VOLUME: Byte = 0x06
    const val TYPE_COMMAND_PLAYSTATE: Byte = 0x07
    const val TYPE_IDENTIFY_CHIRP: Byte = 0x08
    const val TYPE_AUTO_SYNC_ALIGN: Byte = 0x09
    const val TYPE_CONFIG_UPDATE: Byte = 0x0A
    const val TYPE_MASTER_STATS: Byte = 0x0B
    const val TYPE_SPEAKER_TUNING_UPDATE: Byte = 0x0C

    const val SAMPLE_RATE = 44100
    const val CHANNELS = 2
    const val BYTES_PER_SAMPLE = 2 // 16-bit PCM
    const val SAMPLES_PER_FRAME = 512 // ~11.6ms per chunk
    const val AUDIO_PAYLOAD_SIZE = SAMPLES_PER_FRAME * CHANNELS * BYTES_PER_SAMPLE // 2048 bytes

    // Header size:
    // magic (2) + type (1) + flags (1) + seqNumber (8) + timestampNanos (8) + payloadLen (2) = 22 bytes
    const val HEADER_SIZE = 22
    const val MAX_PACKET_SIZE = HEADER_SIZE + AUDIO_PAYLOAD_SIZE + 64

    fun createAudioPacket(
        seq: Long,
        timestampNanos: Long,
        audioBytes: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + length).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_AUDIO_DATA)
        buffer.put(0.toByte()) // flags
        buffer.putLong(seq)
        buffer.putLong(timestampNanos) // Presentation Timestamp (PTS)
        buffer.putShort(length.toShort())
        buffer.put(audioBytes, offset, length)
        return buffer.array()
    }

    fun createAutoSyncPacket(targetPlayNanoTime: Long, delayOffsetMs: Int): ByteArray {
        val payload = "$targetPlayNanoTime|$delayOffsetMs".toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_AUTO_SYNC_ALIGN)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(targetPlayNanoTime)
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    fun createConfigPacket(latencyMode: String, audioProfile: String): ByteArray {
        val payload = "$latencyMode|$audioProfile".toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_CONFIG_UPDATE)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(System.nanoTime())
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    fun createBeaconPacket(masterName: String, masterIp: String, isPlaying: Boolean): ByteArray {
        val payload = "$masterName|$masterIp|$isPlaying".toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_MASTER_BEACON)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(System.nanoTime())
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    fun createMasterStatsPacket(
        deviceName: String,
        deviceModel: String,
        ipAddress: String,
        batteryPercent: Int,
        isCharging: Boolean,
        wifiSignalDbm: Int,
        wifiSignalLevel: Int,
        wifiSsid: String,
        wifiLinkSpeedMbps: Int,
        masterVolumePercent: Int,
        isMuted: Boolean,
        nowPlayingTitle: String,
        nowPlayingArtist: String,
        isPlaying: Boolean,
        audioProfile: String,
        latencyMode: String,
        activeBitrateKbps: Int,
        connectedSpeakersCount: Int
    ): ByteArray {
        val payload = listOf(
            deviceName,
            deviceModel,
            ipAddress,
            batteryPercent.toString(),
            isCharging.toString(),
            wifiSignalDbm.toString(),
            wifiSignalLevel.toString(),
            wifiSsid,
            wifiLinkSpeedMbps.toString(),
            masterVolumePercent.toString(),
            isMuted.toString(),
            nowPlayingTitle,
            nowPlayingArtist,
            isPlaying.toString(),
            audioProfile,
            latencyMode,
            activeBitrateKbps.toString(),
            connectedSpeakersCount.toString()
        ).joinToString("|").toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_MASTER_STATS)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(System.nanoTime())
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    fun createAnnouncePacket(
        speakerId: String,
        speakerName: String,
        battery: Int,
        isCharging: Boolean = false,
        wifiSignalDbm: Int = -52,
        wifiSignalLevel: Int = 4,
        volumePercent: Int = 90,
        isMuted: Boolean = false,
        channel: String = "STEREO_ALL",
        deviceModel: String = "Satellite Speaker"
    ): ByteArray {
        val payload = listOf(
            speakerId,
            speakerName,
            battery.toString(),
            isCharging.toString(),
            wifiSignalDbm.toString(),
            wifiSignalLevel.toString(),
            volumePercent.toString(),
            isMuted.toString(),
            channel,
            deviceModel
        ).joinToString("|").toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_ANNOUNCE_SPEAKER)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(System.nanoTime())
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    fun createSpeakerTuningPacket(
        speakerId: String,
        channel: String,
        zoneId: String,
        audioProfile: String,
        eqEnabled: Boolean,
        eqPreset: String,
        band60: Float,
        band250: Float,
        band1k: Float,
        band4k: Float,
        band12k: Float,
        fineTrimMs: Int,
        volume: Float,
        isMuted: Boolean
    ): ByteArray {
        val payload = listOf(
            speakerId,
            channel,
            zoneId,
            audioProfile,
            eqEnabled.toString(),
            eqPreset,
            band60.toString(),
            band250.toString(),
            band1k.toString(),
            band4k.toString(),
            band12k.toString(),
            fineTrimMs.toString(),
            volume.toString(),
            isMuted.toString()
        ).joinToString("|").toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(TYPE_SPEAKER_TUNING_UPDATE)
        buffer.put(0.toByte())
        buffer.putLong(0L)
        buffer.putLong(System.nanoTime())
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        return buffer.array()
    }
}
