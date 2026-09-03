package com.example.audio.synth

import com.example.audio.mesh.MeshProtocol
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SynthMode {
    FUNK_BEAT,
    ACOUSTIC_CLICK,
    SPATIAL_SWEEP
}

class SynthesizerAudioGenerator {
    private var sampleCounter: Long = 0
    var mode: SynthMode = SynthMode.FUNK_BEAT

    // Tempo in BPM (e.g. 124 BPM)
    private val bpm = 124.0
    private val sampleRate = MeshProtocol.SAMPLE_RATE.toDouble()
    private val samplesPerBeat = (60.0 / bpm * sampleRate).toLong()

    fun generatePcmFrame(buffer: ByteArray, offset: Int, length: Int): Int {
        val numSamples = length / (MeshProtocol.CHANNELS * MeshProtocol.BYTES_PER_SAMPLE)
        val byteBuffer = ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = sampleCounter + i
            var left = 0.0
            var right = 0.0

            when (mode) {
                SynthMode.FUNK_BEAT -> {
                    // Beat division: 4 beats per bar
                    val beatIndex = (t / samplesPerBeat) % 4
                    val sampleInBeat = (t % samplesPerBeat).toDouble()
                    val beatProgress = sampleInBeat / samplesPerBeat

                    // 1. Kick Drum on beats 0, 1, 2, 3 (decay envelope)
                    val kickEnv = exp(-beatProgress * 14.0)
                    val kickFreq = 140.0 * exp(-beatProgress * 20.0) + 48.0
                    val kick = sin(2.0 * PI * kickFreq * (sampleInBeat / sampleRate)) * kickEnv

                    // 2. Hi-Hat on 8th notes (half-beat)
                    val sampleIn8th = (t % (samplesPerBeat / 2)).toDouble()
                    val hatProgress = sampleIn8th / (samplesPerBeat / 2.0)
                    val hatNoise = (Math.random() * 2.0 - 1.0) * exp(-hatProgress * 30.0)

                    // 3. Bass Synth note with melodic progression
                    val notes = doubleArrayOf(110.0, 130.81, 146.83, 164.81) // A2, C3, D3, E3
                    val bassFreq = notes[beatIndex.toInt()]
                    val bassEnv = 0.5 + 0.5 * sin(2.0 * PI * 4.0 * (sampleInBeat / sampleRate))
                    val bass = sin(2.0 * PI * bassFreq * (t.toDouble() / sampleRate)) * 0.35 * bassEnv

                    // 4. Stereo chord pad
                    val padLeft = sin(2.0 * PI * 220.0 * (t.toDouble() / sampleRate)) * 0.15
                    val padRight = sin(2.0 * PI * 330.0 * (t.toDouble() / sampleRate)) * 0.15

                    left = (kick * 0.75 + hatNoise * 0.25 + bass * 0.5 + padLeft)
                    right = (kick * 0.75 + hatNoise * 0.25 + bass * 0.5 + padRight)
                }

                SynthMode.ACOUSTIC_CLICK -> {
                    // Crisp 1kHz click every 0.5 seconds
                    val clickInterval = (sampleRate * 0.5).toLong()
                    val clickSample = t % clickInterval
                    if (clickSample < (sampleRate * 0.015)) { // 15ms transient
                        val decay = exp(-clickSample / (sampleRate * 0.003))
                        val tone = sin(2.0 * PI * 1200.0 * (clickSample / sampleRate)) * decay
                        left = tone * 0.9
                        right = tone * 0.9
                    }
                }

                SynthMode.SPATIAL_SWEEP -> {
                    // Alternates Left only for 1s, Right only for 1s, Both for 1s
                    val cycleSamples = (sampleRate * 3.0).toLong()
                    val pos = t % cycleSamples
                    val tone = sin(2.0 * PI * 440.0 * (t / sampleRate)) * 0.6
                    when {
                        pos < sampleRate -> { // Left
                            left = tone
                            right = 0.0
                        }
                        pos < sampleRate * 2 -> { // Right
                            left = 0.0
                            right = tone
                        }
                        else -> { // Both
                            left = tone * 0.7
                            right = tone * 0.7
                        }
                    }
                }
            }

            // Clamp and convert to 16-bit PCM (-32768 to 32767)
            val leftShort = (left.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            val rightShort = (right.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

            byteBuffer.putShort(leftShort)
            byteBuffer.putShort(rightShort)
        }

        sampleCounter += numSamples
        return length
    }

    fun reset() {
        sampleCounter = 0
    }
}
