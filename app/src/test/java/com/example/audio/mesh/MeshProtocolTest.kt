package com.example.audio.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshProtocolTest {

    @Test
    fun audioPacket_roundTripsHeaderAndPayload() {
        val pcm = ByteArray(MeshProtocol.AUDIO_PAYLOAD_SIZE) { (it % 256).toByte() }
        val seq = 42L
        val pts = 1_000_000_000L

        val packet = MeshProtocol.createAudioPacket(seq, pts, pcm, 0, pcm.size)
        assertTrue(MeshProtocol.isValidMagic(packet))

        val header = MeshProtocol.parseHeader(packet)
        assertNotNull(header)
        assertEquals(MeshProtocol.TYPE_AUDIO_DATA, header!!.type)
        assertEquals(seq, header.seq)
        assertEquals(pts, header.timestampNanos)
        assertEquals(pcm.size, header.payloadLength)

        val payloadStart = MeshProtocol.HEADER_SIZE
        val recovered = packet.copyOfRange(payloadStart, payloadStart + header.payloadLength)
        assertTrue(pcm.contentEquals(recovered))
    }

    @Test
    fun beaconPacket_containsUtf8Fields() {
        val packet = MeshProtocol.createBeaconPacket("Living Room", "192.168.1.10", true)
        val header = MeshProtocol.parseHeader(packet)
        assertNotNull(header)
        assertEquals(MeshProtocol.TYPE_MASTER_BEACON, header!!.type)

        val text = String(
            packet,
            MeshProtocol.HEADER_SIZE,
            header.payloadLength,
            Charsets.UTF_8
        )
        assertEquals("Living Room|192.168.1.10|true", text)
    }

    @Test
    fun parseHeader_rejectsShortOrBadMagic() {
        assertNull(MeshProtocol.parseHeader(byteArrayOf(1, 2, 3)))
        assertNull(MeshProtocol.parseHeader(ByteArray(MeshProtocol.HEADER_SIZE)))
        assertFalse(MeshProtocol.isValidMagic(byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun createAudioPacket_clampsLengthToAvailableBytes() {
        val pcm = byteArrayOf(1, 2, 3, 4, 5)
        val packet = MeshProtocol.createAudioPacket(1L, 2L, pcm, 2, 100)
        val header = MeshProtocol.parseHeader(packet)!!
        assertEquals(3, header.payloadLength)
    }
}
