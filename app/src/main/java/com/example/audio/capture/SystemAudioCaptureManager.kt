package com.example.audio.capture

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import com.example.audio.mesh.MeshProtocol
import com.example.model.AudioSourceType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class SystemAudioCaptureManager(
    private val onPcmData: (ByteArray, Int, Float, List<Float>) -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile private var isRecording = false
    private var captureThread: Thread? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun startCapture(
        sourceType: AudioSourceType,
        mediaProjection: MediaProjection? = null
    ) {
        stopCapture()

        val sampleRate = MeshProtocol.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, MeshProtocol.AUDIO_PAYLOAD_SIZE * 4)

        try {
            audioRecord = when {
                sourceType == AudioSourceType.SYSTEM_AUDIO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null -> {
                    val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build()

                    AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(config)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioFormat)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                }

                sourceType == AudioSourceType.PARTY_MIC -> {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                else -> {
                    onError("System audio capture requires Android 10 or newer and playback access.")
                    return
                }
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize AudioRecord. Verify permissions.")
                audioRecord?.release()
                audioRecord = null
                return
            }

            val record = audioRecord ?: return
            record.startRecording()
            isRecording = true

            captureThread = Thread({
                val tempBuffer = ByteArray(MeshProtocol.AUDIO_PAYLOAD_SIZE)
                val byteBuffer = ByteBuffer.wrap(tempBuffer).order(ByteOrder.LITTLE_ENDIAN)

                while (isRecording) {
                    val bytesRead = record.read(tempBuffer, 0, tempBuffer.size)
                    if (bytesRead > 0) {
                        // Calculate RMS level and 8 frequency-like energy bands for visualizer
                        var sumSquares = 0.0
                        val samples = bytesRead / 2
                        val bandEnergies = FloatArray(8) { 0f }

                        byteBuffer.rewind()
                        for (i in 0 until samples) {
                            val sample = byteBuffer.short.toDouble()
                            sumSquares += sample * sample
                            val bandIndex = (i % 8)
                            bandEnergies[bandIndex] += abs(sample.toFloat()) / 32768f
                        }

                        val rms = (sqrt(sumSquares / samples) / 32768.0).toFloat().coerceIn(0f, 1f)
                        val bands = bandEnergies.map { (it / (samples / 8f)).coerceIn(0.05f, 1f) }

                        onPcmData(tempBuffer, bytesRead, rms, bands)
                    } else if (bytesRead < 0 && isRecording) {
                        isRecording = false
                        onError("Audio capture ended ($bytesRead). Grant access again to resume.")
                    }
                }
            }, "SoundMesh-AudioCaptureThread").apply { start() }

        } catch (e: Exception) {
            Log.e("AudioCapture", "Error starting audio capture", e)
            onError("Capture error: ${e.localizedMessage}")
            stopCapture()
        }
    }

    @Synchronized
    fun stopCapture() {
        isRecording = false
        val record = audioRecord
        audioRecord = null
        // Stop unblocks a blocking read; interrupt alone does not stop AudioRecord.
        try { record?.stop() } catch (_: Exception) {}
        val thread = captureThread
        captureThread = null
        thread?.interrupt()
        if (thread != null && thread !== Thread.currentThread()) {
            try { thread.join(1000) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
        try { record?.release() } catch (e: Exception) { Log.w("AudioCapture", "Release failed", e) }
    }
}
