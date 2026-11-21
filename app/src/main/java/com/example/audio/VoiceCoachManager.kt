package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.example.chess.BotProfile
import com.example.chess.BotVoiceProfile
import com.example.data.local.AppSettings
import com.example.data.local.VoiceMode
import java.util.Locale

/**
 * Android TextToSpeech wrapper with VoiceMode (Off/Basic/Full), per-bot distinct voice selection,
 * pitch/rate modulation, and anti-spam cooldown.
 */
class VoiceCoachManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false
    @Volatile
    private var lastSpokenAtMs = 0L
    @Volatile
    private var defaultVoice: Voice? = null
    @Volatile
    private var cachedEnglishVoices: List<Voice> = emptyList()

    private fun ensureInitialized(onReady: (() -> Unit)? = null) {
        if (tts != null && ready) {
            onReady?.invoke()
            return
        }
        if (tts != null) return
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        engine.language = Locale.ENGLISH
                        defaultVoice = runCatching { engine.defaultVoice }.getOrNull()
                        cachedEnglishVoices = runCatching {
                            engine.voices
                                ?.filter {
                                    it.locale.language.equals("en", ignoreCase = true) &&
                                        !it.isNetworkConnectionRequired
                                }
                                ?.sortedBy { it.name }
                                ?: emptyList()
                        }.getOrDefault(emptyList())
                    }
                    ready = true
                    onReady?.invoke()
                }
            }
        }
    }

    fun speakBot(
        text: String,
        settings: AppSettings,
        bot: BotProfile? = null,
        isKeyAlert: Boolean = false,
    ) {
        if (settings.voiceMode == VoiceMode.OFF) return
        if (settings.voiceMode == VoiceMode.BASIC && !isKeyAlert) return
        if (!settings.botVoiceEnabled || settings.voiceVolume <= 0.02f || text.isBlank()) return
        val vp = bot?.voiceProfile ?: BotVoiceProfile()
        speakInternal(
            text = text,
            volume = settings.voiceVolume,
            pitch = vp.pitch.coerceIn(0.65f, 1.45f),
            rate = (vp.rate * settings.voiceSpeed).coerceIn(0.65f, 1.75f),
            preferredVoice = pickBotVoice(vp),
        )
    }

    fun speakTrainer(text: String, settings: AppSettings, isKeyAlert: Boolean = true) {
        if (settings.voiceMode == VoiceMode.OFF) return
        if (settings.voiceMode == VoiceMode.BASIC && !isKeyAlert) return
        if (!settings.trainerVoiceEnabled || settings.voiceVolume <= 0.02f || text.isBlank()) return
        speakInternal(
            text = text,
            volume = settings.voiceVolume,
            pitch = 0.96f,
            rate = (0.98f * settings.voiceSpeed).coerceIn(0.7f, 1.6f),
            preferredVoice = defaultVoice,
        )
    }

    private fun pickBotVoice(profile: BotVoiceProfile): Voice? {
        val pool = cachedEnglishVoices
        if (pool.isEmpty()) return defaultVoice
        val targetTag = profile.preferredLocaleTag
        val localeMatches = pool.filter {
            it.locale.toLanguageTag().equals(targetTag, ignoreCase = true)
        }
        val candidates = if (localeMatches.isNotEmpty()) localeMatches else pool
        return candidates[profile.timbreSlot.coerceAtLeast(0) % candidates.size]
    }

    private fun speakInternal(
        text: String,
        volume: Float,
        pitch: Float,
        rate: Float,
        preferredVoice: Voice?,
    ) {
        val now = System.currentTimeMillis()
        if (now - lastSpokenAtMs < 1400L) return
        lastSpokenAtMs = now
        runCatching {
            ensureInitialized {
                val engine = tts ?: return@ensureInitialized
                if (preferredVoice != null) {
                    runCatching { engine.voice = preferredVoice }
                } else {
                    runCatching { engine.language = Locale.ENGLISH }
                }
                engine.setPitch(pitch)
                engine.setSpeechRate(rate)
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
                }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "gamebit_${System.currentTimeMillis()}")
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
