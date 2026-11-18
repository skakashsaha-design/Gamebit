package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class ChessSoundType {
    MOVE,
    CAPTURE,
    CHECK,
    CASTLE,
    PROMOTION,
    GAME_END,
    ERROR,
}

/**
 * Synthesizes lightweight PCM wood-click & chime audio in memory without bundling heavy MP3 files.
 */
class SoundAndHapticsManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun play(
        type: ChessSoundType,
        soundEnabled: Boolean,
        volume: Float,
        hapticsEnabled: Boolean,
        lowMemoryMode: Boolean,
    ) {
        if (hapticsEnabled && !lowMemoryMode) {
            triggerHaptic(type)
        }
        if (soundEnabled && volume > 0.02f) {
            scope.launch { playTone(type, volume.coerceIn(0f, 1f)) }
        }
    }

    private fun triggerHaptic(type: ChessSoundType) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val ms = when (type) {
            ChessSoundType.CAPTURE, ChessSoundType.CHECK -> 26L
            ChessSoundType.GAME_END -> 45L
            ChessSoundType.ERROR -> 35L
            else -> 12L
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amp = when (type) {
                ChessSoundType.CAPTURE, ChessSoundType.CHECK -> 140
                ChessSoundType.GAME_END -> 180
                else -> 70
            }
            v.vibrate(VibrationEffect.createOneShot(ms, amp))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    private fun playTone(type: ChessSoundType, volume: Float) {
        runCatching {
            val sampleRate = 22050
            val durationMs = when (type) {
                ChessSoundType.GAME_END -> 190
                ChessSoundType.CHECK -> 110
                else -> 55
            }
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            val freq = when (type) {
                ChessSoundType.MOVE -> 340.0
                ChessSoundType.CAPTURE -> 230.0
                ChessSoundType.CHECK -> 620.0
                ChessSoundType.CASTLE -> 410.0
                ChessSoundType.PROMOTION -> 740.0
                ChessSoundType.GAME_END -> 520.0
                ChessSoundType.ERROR -> 150.0
            }

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 32.0)
                val wave = sin(2.0 * PI * freq * t) * env
                samples[i] = (wave * Short.MAX_VALUE * 0.45 * volume).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 30L)
            track.release()
        }
    }
}
