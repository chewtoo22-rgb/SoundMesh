package com.example.audio.mesh

/** Clock math uses monotonic clocks; no wall clock or future playback anchor. */
object ClockSync {
    data class Sample(val masterMinusSpeakerNanos: Long, val roundTripNanos: Long)

    fun measure(masterSend: Long, speakerReceive: Long, speakerSend: Long, masterReceive: Long): Sample? {
        if (masterReceive < masterSend || speakerSend < speakerReceive) return null
        val roundTrip = (masterReceive - masterSend) - (speakerSend - speakerReceive)
        if (roundTrip !in 0L..1_000_000_000L) return null
        val offset = ((masterSend - speakerReceive) + (masterReceive - speakerSend)) / 2
        return Sample(offset, roundTrip)
    }

    /** Use the least congested of the recent exchanges instead of chasing Wi-Fi jitter. */
    class Filter {
        private val samples = ArrayDeque<Sample>()

        fun add(sample: Sample): Sample {
            samples.addLast(sample)
            if (samples.size > 8) samples.removeFirst()
            return samples.minBy { it.roundTripNanos }
        }
    }
}

class RemoteClock {
    @Volatile private var offsetNanos = 0L
    @Volatile var hasEstimate = false
        private set
    private var measured = false
    private var lastMeasurement = -1L

    @Synchronized fun bootstrap(masterSendNanos: Long, localReceiveNanos: Long) {
        if (!measured) {
            offsetNanos = masterSendNanos - localReceiveNanos
            hasEstimate = true
        }
    }

    @Synchronized fun update(sequence: Long, masterMinusSpeakerNanos: Long) {
        if (sequence <= lastMeasurement) return
        offsetNanos = masterMinusSpeakerNanos
        lastMeasurement = sequence
        measured = true
        hasEstimate = true
    }

    fun localPresentationTime(masterPresentationNanos: Long, trimMs: Int): Long =
        masterPresentationNanos - offsetNanos + trimMs * 1_000_000L

    @Synchronized fun reset() {
        offsetNanos = 0L
        hasEstimate = false
        measured = false
        lastMeasurement = -1L
    }
}

/** A restarted master reuses its IP but begins a new sequence and clock epoch. */
class StreamSession {
    private var sessionId: String? = null
    private var lastSequence = -1L

    @Synchronized fun announce(id: String): Boolean {
        if (id.isBlank() || id == sessionId) return false
        sessionId = id
        lastSequence = -1L
        return true
    }

    @Synchronized fun accept(sequence: Long): Boolean {
        if (sequence <= lastSequence) return false
        lastSequence = sequence
        return true
    }

    @Synchronized fun reset() {
        sessionId = null
        lastSequence = -1L
    }
}
