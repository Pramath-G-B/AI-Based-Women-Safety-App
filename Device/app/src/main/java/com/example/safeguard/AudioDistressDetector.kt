package com.example.safeguard

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

class AudioDistressDetector {

    @Volatile
    var audioLevel: Int = 0
        private set

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            audioLevel = 0
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioLevel = 0
                return
            }

            isRunning = true
            audioRecord?.startRecording()

            worker = Thread {
                val buffer = ShortArray(1024)

                while (isRunning) {
                    try {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                val sample = buffer[i].toDouble()
                                sum += sample * sample
                            }

                            val rms = sqrt(sum / read).toInt()
                            audioLevel = rms.coerceIn(0, 10000)
                        }
                    } catch (_: Exception) {
                        audioLevel = 0
                    }
                }
            }

            worker?.start()

        } catch (_: Exception) {
            audioLevel = 0
        }
    }

    fun stop() {
        isRunning = false

        try {
            worker?.join(300)
        } catch (_: Exception) {
        }

        audioRecord?.apply {
            try {
                stop()
            } catch (_: Exception) {
            }

            try {
                release()
            } catch (_: Exception) {
            }
        }

        audioRecord = null
        audioLevel = 0
    }
}