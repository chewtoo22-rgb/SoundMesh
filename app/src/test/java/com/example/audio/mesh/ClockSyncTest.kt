package com.example.audio.mesh

import org.junit.Assert.*
import org.junit.Test

class ClockSyncTest {
    @Test fun fourTimestampExchangeRemovesNetworkAndProcessingDelay() {
        // Master is 5 seconds ahead; 10 ms outbound, 20 ms processing, 10 ms inbound.
        val result = ClockSync.measure(6_000_000_000L, 1_010_000_000L, 1_030_000_000L, 6_040_000_000L)!!
        assertEquals(5_000_000_000L, result.masterMinusSpeakerNanos)
        assertEquals(20_000_000L, result.roundTripNanos)
    }

    @Test fun clocksCanHaveNegativeOffsets() {
        val result = ClockSync.measure(1_000_000_000L, 6_010_000_000L, 6_010_000_000L, 1_020_000_000L)!!
        assertEquals(-5_000_000_000L, result.masterMinusSpeakerNanos)
    }

    @Test fun invalidOrStaleExchangeIsRejected() {
        assertNull(ClockSync.measure(100, 100, 99, 110))
        assertNull(ClockSync.measure(100, 100, 130, 110))
        assertNull(ClockSync.measure(100, 100, 100, 2_000_000_000))
    }

    @Test fun lowLatencyExchangeIsKeptWhileRecentCongestionIsIgnored() {
        val filter = ClockSync.Filter()
        val best = ClockSync.Sample(5_000_000, 2_000_000)
        assertEquals(best, filter.add(best))
        assertEquals(best, filter.add(ClockSync.Sample(50_000_000, 100_000_000)))
        // The old sample eventually ages out so clock drift can be tracked.
        repeat(8) { filter.add(ClockSync.Sample(6_000_000, 3_000_000)) }
        assertEquals(6_000_000L, filter.add(ClockSync.Sample(7_000_000, 4_000_000)).masterMinusSpeakerNanos)
    }

    @Test fun futurePresentationKeepsFullJitterBufferAfterMeasuredSync() {
        val clock = RemoteClock()
        clock.update(1, 5_000_000_000L)
        // Align/buffer changes must never be fed into update(): PTS stays 40 ms ahead.
        assertEquals(1_040_000_000L, clock.localPresentationTime(6_040_000_000L, 0))
        assertEquals(1_065_000_000L, clock.localPresentationTime(6_040_000_000L, 25))
        clock.bootstrap(6_000_000_000L, 1_050_000_000L)
        assertEquals(1_040_000_000L, clock.localPresentationTime(6_040_000_000L, 0))
        clock.update(0, 10_000_000_000L) // delayed UDP reply cannot undo a newer measurement
        assertEquals(1_040_000_000L, clock.localPresentationTime(6_040_000_000L, 0))
    }

    @Test fun restartAcceptsLowerSequenceAndClearsOldClock() {
        val session = StreamSession()
        val clock = RemoteClock()
        assertTrue(session.announce("first"))
        assertTrue(session.accept(100))
        assertFalse(session.accept(100))
        assertFalse(session.announce("first"))
        assertFalse(session.accept(1))
        clock.update(100, 5_000_000_000)
        assertTrue(session.announce("restarted"))
        clock.reset()
        assertFalse(clock.hasEstimate)
        assertTrue(session.accept(1))
        clock.update(1, 3_000_000_000)
        assertEquals(1_000_000_000L, clock.localPresentationTime(4_000_000_000, 0))
    }
}
