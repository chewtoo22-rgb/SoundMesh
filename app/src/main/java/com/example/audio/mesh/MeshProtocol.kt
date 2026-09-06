package com.example.audio.mesh

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SoundMesh binary wire format over UDP (multicast + unicast).
 *
 * Packet layout (big-endian):
 * ```
 * magic[2] = 'S''M'
 * type[1]
 * flags[1]
 * seq[8]
 * timestampNanos[8]   // PTS / event time
 * payloadLen[2]
 * payload[payloadLen]
 * ```
 */
object MeshProtocol {
    const val MULTICAST_GROUP = "239.255.42.99"
    const val AUDIO_PORT = 9876
    const val DISCOVERY_PORT = 9877

    const val MAGIC_BYTE_1: Byte = 0x53 // 'S'
    const val MAGIC_BYTE_2: Byte = 0x4D // 'M'

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
    const val SAMPLES_PER_FRAME = 512 // ~11.6 ms @ 44.1 kHz
    const val AUDIO_PAYLOAD_SIZE = SAMPLES_PER_FRAME * CHANNELS * BYTES_PER_SAMPLE // 2048

    /** magic(2) + type(1) + flags(1) + seq(8) + timestamp(8) + payloadLen(2) */
    const val HEADER_SIZE = 22
    const val MAX_PACKET_SIZE = HEADER_SIZE + AUDIO_PAYLOAD_SIZE + 64
    const val MAX_PAYLOAD_LENGTH = 32_000 // soft cap for text control packets

    data class Header(
        val type: Byte,
        val flags: Byte,
        val seq: Long,
        val timestampNanos: Long,
        val payloadLength: Int
    )

    private fun writeHeader(
        type: Byte,
        seq: Long,
        timestampNanos: Long,
        payloadLength: Int,
        flags: Byte = 0
    ): ByteBuffer {
        require(payloadLength in 0..MAX_PAYLOAD_LENGTH) {
            "payloadLength out of range: $payloadLength"
        }
        val buffer = ByteBuffer.allocate(HEADER_SIZE + payloadLength).order(ByteOrder.BIG_ENDIAN)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(MAGIC_BYTE_2)
        buffer.put(type)
        buffer.put(flags)
        buffer.putLong(seq)
        buffer.putLong(timestampNanos)
        buffer.putShort(payloadLength.toShort())
        return buffer
    }

    private fun packetWithUtf8Payload(
        type: Byte,
        payload: String,
        seq: Long = 0L,
        timestampNanos: Long = System.nanoTime()
    ): ByteArray {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val buffer = writeHeader(type, seq, timestampNanos, bytes.size)
        buffer.put(bytes)
        return buffer.array()
    }

    /** Returns null if the buffer is too short or magic does not match. */
    fun parseHeader(data: ByteArray, offset: Int = 0): Header? {
        if (data.size - offset < HEADER_SIZE) return null
        if (data[offset] != MAGIC_BYTE_1 || data[offset + 1] != MAGIC_BYTE_2) return null
        val buffer = ByteBuffer.wrap(data, offset, data.size - offset).order(ByteOrder.BIG_ENDIAN)
        buffer.position(offset + 2)
        val type = buffer.get()
        val flags = buffer.get()
        val seq = buffer.long
        val timestampNanos = buffer.long
        val payloadLength = buffer.short.toInt() and 0xFFFF
        if (payloadLength > MAX_PAYLOAD_LENGTH) return null
        if (data.size - offset < HEADER_SIZE + payloadLength) return null
        return Header(type, flags, seq, timestampNanos, payloadLength)
    }

    fun isValidMagic(data: ByteArray, offset: Int = 0): Boolean {
        return data.size - offset >= 2 &&
            data[offset] == MAGIC_BYTE_1 &&
            data[offset + 1] == MAGIC_BYTE_2
    }

    fun createAudioPacket(
        seq: Long,
        timestampNanos: Long,
        audioBytes: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        val safeLen = length.coerceIn(0, audioBytes.size - offset)
        val buffer = writeHeader(TYPE_AUDIO_DATA, seq, timestampNanos, safeLen)
        buffer.put(audioBytes, offset, safeLen)
        return buffer.array()
    }

    fun createAutoSyncPacket(targetPlayNanoTime: Long, delayOffsetMs: Int): ByteArray {
        return packetWithUtf8Payload(
            type = TYPE_AUTO_SYNC_ALIGN,
            payload = "$targetPlayNanoTime|$delayOffsetMs",
            timestampNanos = targetPlayNanoTime
        )
    }

    fun createConfigPacket(latencyMode: String, audioProfile: String): ByteArray {
        return packetWithUtf8Payload(
            type = TYPE_CONFIG_UPDATE,
            payload = "$latencyMode|$audioProfile"
        )
    }

    fun createBeaconPacket(masterName: String, masterIp: String, isPlaying: Boolean): ByteArray {
        return packetWithUtf8Payload(
            type = TYPE_MASTER_BEACON,
            payload = "$masterName|$masterIp|$isPlaying"
        )
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
        ).joinToString("|")
        return packetWithUtf8Payload(TYPE_MASTER_STATS, payload)
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
        ).joinToString("|")
        return packetWithUtf8Payload(TYPE_ANNOUNCE_SPEAKER, payload)
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
        ).joinToString("|")
        return packetWithUtf8Payload(TYPE_SPEAKER_TUNING_UPDATE, payload)
    }
}
