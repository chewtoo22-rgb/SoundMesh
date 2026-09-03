package com.example.audio.dsp

import com.example.model.AudioProfile
import com.example.model.EqualizerSettings
import com.example.model.SpeakerChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance DSP audio processor for:
 * 1. 5-Band Biquad Graphic Equalizer (60Hz, 250Hz, 1kHz, 4kHz, 12kHz)
 * 2. Audio Profiles (Virtual 5.1 Surround, All Stereo, Bass Blast, Vocal Boost, Wide Stage)
 * 3. Spatial Channel Extraction (Center, Surrounds, Subwoofer LFE, Left, Right)
 * 4. Anti-clipping soft limiter
 */
class AudioDspEngine(
    private val sampleRate: Int = 44100
) {
    // 5 Biquad filters for Left and 5 for Right
    private val eqFiltersLeft = Array(5) { BiquadFilter() }
    private val eqFiltersRight = Array(5) { BiquadFilter() }

    // Center frequencies for the 5 equalizer bands
    private val bandFrequencies = floatArrayOf(60f, 250f, 1000f, 4000f, 12000f)
    private val bandQ = floatArrayOf(1.0f, 1.1f, 1.2f, 1.1f, 1.0f)

    // Subwoofer 2nd-order Low-Pass Filter (< 120Hz)
    private val subLpfLeft = BiquadFilter().apply { configureLowPass(120f, 0.707f, sampleRate) }
    private val subLpfRight = BiquadFilter().apply { configureLowPass(120f, 0.707f, sampleRate) }

    // Surround Haas decorrelation delay ring buffer (~12ms delay = ~530 samples)
    private val surroundDelayBufferSize = (sampleRate * 0.012).toInt().coerceAtLeast(128)
    private val surroundDelayRingLeft = ShortArray(surroundDelayBufferSize)
    private val surroundDelayRingRight = ShortArray(surroundDelayBufferSize)
    private var surroundDelayIndex = 0

    @Volatile
    var currentProfile: AudioProfile = AudioProfile.ALL_STEREO

    @Volatile
    var isEqEnabled: Boolean = true

    init {
        updateEqualizer(EqualizerSettings())
    }

    /**
     * Recalculates filter coefficients for the 5-band Equalizer
     */
    fun updateEqualizer(settings: EqualizerSettings) {
        isEqEnabled = settings.isEnabled
        val gains = floatArrayOf(
            settings.band60Hz,
            settings.band250Hz,
            settings.band1kHz,
            settings.band4kHz,
            settings.band12kHz
        )

        for (i in 0 until 5) {
            val f0 = bandFrequencies[i]
            val q = bandQ[i]
            val gainDb = gains[i]
            eqFiltersLeft[i].configurePeaking(f0, q, gainDb, sampleRate)
            eqFiltersRight[i].configurePeaking(f0, q, gainDb, sampleRate)
        }
    }

    /**
     * In-place or output processing of a stereo 16-bit PCM buffer
     */
    fun processStereoPcm(
        input: ByteArray,
        length: Int,
        output: ByteArray,
        targetChannel: SpeakerChannel = SpeakerChannel.STEREO_ALL,
        masterGain: Float = 1.0f
    ) {
        val inBuf = ByteBuffer.wrap(input, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        val outBuf = ByteBuffer.wrap(output, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        val numFrames = length / 4 // 2 bytes left + 2 bytes right = 4 bytes per frame

        for (i in 0 until numFrames) {
            var left = inBuf.short.toFloat()
            var right = inBuf.short.toFloat()

            // 1. Apply Graphic Equalizer if enabled
            if (isEqEnabled) {
                for (b in 0 until 5) {
                    left = eqFiltersLeft[b].process(left)
                    right = eqFiltersRight[b].process(right)
                }
            }

            // 2. Audio Profiles Enhancement
            when (currentProfile) {
                AudioProfile.ALL_STEREO -> {
                    // Transparent studio passthrough
                }
                AudioProfile.WIDE_STAGE -> {
                    // Mid-Side 3D Stereo Widening
                    val mid = (left + right) * 0.5f
                    val side = (left - right) * 0.5f
                    // Boost side component by 40% for expansive spatial perception
                    val boostedSide = side * 1.45f
                    left = mid + boostedSide
                    right = mid - boostedSide
                }
                AudioProfile.BASS_BLAST -> {
                    // Dynamic bass harmonic saturation
                    val bassMono = (left + right) * 0.5f
                    val saturatedBass = softSaturate(bassMono * 1.5f) * 0.4f
                    left += saturatedBass
                    right += saturatedBass
                }
                AudioProfile.VOCAL_BOOST -> {
                    // Speech and dialogue lift
                    val centerMono = (left + right) * 0.5f
                    left = (left * 0.7f) + (centerMono * 0.45f)
                    right = (right * 0.7f) + (centerMono * 0.45f)
                }
                AudioProfile.SURROUND_5_1 -> {
                    // Profile-level surround prep
                }
            }

            // 3. Channel Mapping for Multi-Speaker Spatial Deployment
            var finalLeft: Float
            var finalRight: Float

            when (targetChannel) {
                SpeakerChannel.STEREO_ALL -> {
                    finalLeft = left
                    finalRight = right
                }
                SpeakerChannel.LEFT_ONLY -> {
                    finalLeft = left
                    finalRight = 0f
                }
                SpeakerChannel.RIGHT_ONLY -> {
                    finalLeft = 0f
                    finalRight = right
                }
                SpeakerChannel.CENTER -> {
                    // True center channel extraction with slight vocal presence
                    val center = (left + right) * 0.5f
                    finalLeft = center
                    finalRight = center
                }
                SpeakerChannel.SURROUND_LEFT -> {
                    // Haas delayed differential surround channel
                    val delayedL = surroundDelayRingLeft[surroundDelayIndex].toFloat()
                    surroundDelayRingLeft[surroundDelayIndex] = left.toInt().coerceIn(-32768, 32767).toShort()
                    val surroundL = (left - (right * 0.6f)) * 0.75f + (delayedL * 0.25f)
                    finalLeft = surroundL
                    finalRight = 0f
                }
                SpeakerChannel.SURROUND_RIGHT -> {
                    val delayedR = surroundDelayRingRight[surroundDelayIndex].toFloat()
                    surroundDelayRingRight[surroundDelayIndex] = right.toInt().coerceIn(-32768, 32767).toShort()
                    val surroundR = (right - (left * 0.6f)) * 0.75f + (delayedR * 0.25f)
                    finalLeft = 0f
                    finalRight = surroundR
                }
                SpeakerChannel.SUBWOOFER -> {
                    // Low pass < 120Hz LFE for sub phone
                    val mono = (left + right) * 0.5f
                    val subSample = subLpfLeft.process(mono) * 1.8f
                    finalLeft = subSample
                    finalRight = subSample
                }
                SpeakerChannel.MONO_MIX -> {
                    val mono = (left + right) * 0.5f
                    finalLeft = mono
                    finalRight = mono
                }
            }

            surroundDelayIndex = (surroundDelayIndex + 1) % surroundDelayBufferSize

            // 4. Master Volume & Anti-Clipping Soft Limiter
            finalLeft *= masterGain
            finalRight *= masterGain

            val clampedL = softLimiter(finalLeft)
            val clampedR = softLimiter(finalRight)

            outBuf.putShort(clampedL.toInt().toShort())
            outBuf.putShort(clampedR.toInt().toShort())
        }
    }

    /**
     * Soft saturation tanh approximation to warm up sound without harsh clipping
     */
    private fun softSaturate(x: Float): Float {
        val normalized = x / 32768f
        val sat = normalized / sqrt(1.0f + normalized * normalized)
        return sat * 32768f
    }

    /**
     * Transparent brickwall / smooth soft limiter to prevent any integer overflow
     */
    private fun softLimiter(x: Float): Float {
        val absX = kotlin.math.abs(x)
        return if (absX <= 28000f) {
            x
        } else {
            val sign = if (x >= 0f) 1f else -1f
            val excess = absX - 28000f
            sign * (28000f + (4767f * (excess / (excess + 4767f))))
        }
    }

    /**
     * Direct Form II Biquad IIR Filter
     */
    class BiquadFilter {
        private var b0 = 1.0f
        private var b1 = 0.0f
        private var b2 = 0.0f
        private var a1 = 0.0f
        private var a2 = 0.0f

        private var w1 = 0.0f
        private var w2 = 0.0f

        fun configurePeaking(frequency: Float, q: Float, gainDb: Float, sampleRate: Int) {
            if (kotlin.math.abs(gainDb) < 0.05f) {
                // Flat passthrough
                b0 = 1.0f
                b1 = 0.0f
                b2 = 0.0f
                a1 = 0.0f
                a2 = 0.0f
                return
            }

            val a = Math.pow(10.0, (gainDb / 40.0).toDouble()).toFloat()
            val omega = (2.0 * PI * frequency / sampleRate).toFloat()
            val sn = sin(omega.toDouble()).toFloat()
            val cs = cos(omega.toDouble()).toFloat()
            val alpha = sn / (2.0f * q)

            val a0 = 1.0f + alpha / a
            b0 = (1.0f + alpha * a) / a0
            b1 = (-2.0f * cs) / a0
            b2 = (1.0f - alpha * a) / a0
            a1 = (-2.0f * cs) / a0
            a2 = (1.0f - alpha / a) / a0
        }

        fun configureLowPass(cutoffFreq: Float, q: Float, sampleRate: Int) {
            val omega = (2.0 * PI * cutoffFreq / sampleRate).toFloat()
            val sn = sin(omega.toDouble()).toFloat()
            val cs = cos(omega.toDouble()).toFloat()
            val alpha = sn / (2.0f * q)

            val a0 = 1.0f + alpha
            b0 = ((1.0f - cs) / 2.0f) / a0
            b1 = (1.0f - cs) / a0
            b2 = ((1.0f - cs) / 2.0f) / a0
            a1 = (-2.0f * cs) / a0
            a2 = (1.0f - alpha) / a0
        }

        fun process(sample: Float): Float {
            val w0 = sample - a1 * w1 - a2 * w2
            val yn = b0 * w0 + b1 * w1 + b2 * w2
            w2 = w1
            w1 = w0
            return yn
        }
    }
}
