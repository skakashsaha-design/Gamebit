package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.example.data.local.AppSettings
import java.util.Locale

/**
 * Android TextToSpeech wrapper with independent Bot Voice and Coach/Trainer Voice toggles.
 */
class VoiceCoachManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false

    private fun ensureInitialized(onReady: (() -> Unit)? = null) {
        if (tts != null && ready) {
            onReady?.invoke()
            return
        }
        if (tts != null) return
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.ENGLISH
                    ready = true
                    onReady?.invoke()
                }
            }
        }
    }

    fun speakBot(text: String, settings: AppSettings) {
        if (!settings.botVoiceEnabled || settings.voiceVolume <= 0.02f || text.isBlank()) return
        speakInternal(text, settings.voiceVolume, pitch = 1.0f, rate = 1.02f)
    }

    fun speakTrainer(text: String, settings: AppSettings) {
        if (!settings.trainerVoiceEnabled || settings.voiceVolume <= 0.02f || text.isBlank()) return
        speakInternal(text, settings.voiceVolume, pitch = 0.96f, rate = 0.98f)
    }

    private fun speakInternal(text: String, volume: Float, pitch: Float, rate: Float) {
        runCatching {
            ensureInitialized {
                val engine = tts ?: return@ensureInitialized
                engine.setPitch(pitch)
                engine.setSpeechRate(rate)
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
                }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "chesshub_${System.currentTimeMillis()}")
            }
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready = false
        }
    }
}
