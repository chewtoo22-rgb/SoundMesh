package com.example.audio.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MeshProtocolTest {
    @Test
    fun audioPacketRoundTripPreservesHeaderAndPayload() {
        val payload = ByteArray(MeshProtocol.AUDIO_PAYLOAD_SIZE) { it.toByte() }
        val sequence = 42L
        val timestamp = 123_456_789L

        val packet = MeshProtocol.createAudioPacket(
            seq = sequence,
            timestampNanos = timestamp,
            audioBytes = payload,
            offset = 0,
            length = payload.size
        )

        assertEquals(MeshProtocol.HEADER_SIZE + payload.size, packet.size)
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        assertEquals(MeshProtocol.MAGIC_BYTE_1, buffer.get())
        assertEquals(MeshProtocol.MAGIC_BYTE_2, buffer.get())
        assertEquals(MeshProtocol.TYPE_AUDIO_DATA, buffer.get())
        buffer.get()
        assertEquals(sequence, buffer.getLong())
        assertEquals(timestamp, buffer.getLong())
        assertEquals(payload.size.toShort(), buffer.getShort())

        val decoded = ByteArray(buffer.remaining())
        buffer.get(decoded)
        assertTrue(payload.contentEquals(decoded))
    }

    @Test
    fun protocolConstantsFitOneUdpAudioFrame() {
        assertTrue(MeshProtocol.AUDIO_PAYLOAD_SIZE > 0)
        assertTrue(MeshProtocol.MAX_PACKET_SIZE >= MeshProtocol.HEADER_SIZE + MeshProtocol.AUDIO_PAYLOAD_SIZE)
        assertTrue(MeshProtocol.HEADER_SIZE < 64)
    }
}
