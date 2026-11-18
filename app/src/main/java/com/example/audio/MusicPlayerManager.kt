package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.local.AppSettings
import com.example.data.local.MusicTrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

enum class MusicPlayContext {
    MENU,
    GAME,
    TRAINING,
}

data class MusicPlayerUiState(
    val tracks: List<MusicTrackEntity> = emptyList(),
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 180_000L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val filterSource: String = "All", // All, Built-in, My Music
) {
    val currentTrack: MusicTrackEntity?
        get() = tracks.firstOrNull { it.id == currentTrackId } ?: tracks.firstOrNull()
}

/**
 * Full-featured local & built-in ambient music player:
 * - Supports user-picked device audio via Storage Access Framework (`OpenMultipleDocuments`) + MediaPlayer
 * - Supports built-in ambient focus chord loops via lightweight PCM AudioTrack
 * - Honors Music Volume, Menu/Game/Training contextual toggles, Shuffle, Repeat, Seek, and Low-Memory mode
 */
class MusicPlayerManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var progressJob: Job? = null
    private var lastSettings: AppSettings = AppSettings()
    private var activeContext: MusicPlayContext = MusicPlayContext.MENU

    private val builtInTracks = listOf(
        MusicTrackEntity(
            id = "builtin_focus_calm",
            title = "Quiet Board (Ambient Focus)",
            artist = "ChessHub Synth",
            uriString = "builtin://focus_calm",
            isBuiltIn = true,
            durationMs = 180_000L,
            playlistName = "Study Focus",
        ),
        MusicTrackEntity(
            id = "builtin_endgame_drift",
            title = "Endgame Contemplation",
            artist = "ChessHub Synth",
            uriString = "builtin://endgame_drift",
            isBuiltIn = true,
            durationMs = 180_000L,
            playlistName = "Study Focus",
        ),
        MusicTrackEntity(
            id = "builtin_tactics_pulse",
            title = "Tactical Pulse 64",
            artist = "ChessHub Synth",
            uriString = "builtin://tactics_pulse",
            isBuiltIn = true,
            durationMs = 180_000L,
            playlistName = "Classical Lounge",
        ),
    )

    private val _uiState = MutableStateFlow(
        MusicPlayerUiState(
            tracks = builtInTracks,
            currentTrackId = builtInTracks.first().id,
        ),
    )
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    fun syncUserTracks(userTracks: List<MusicTrackEntity>) {
        val combined = builtInTracks + userTracks.filter { !it.isBuiltIn }
        _uiState.update { st ->
            val currentExists = combined.any { it.id == st.currentTrackId }
            st.copy(
                tracks = combined,
                currentTrackId = if (currentExists) st.currentTrackId else combined.firstOrNull()?.id,
            )
        }
    }

    fun applySettings(settings: AppSettings, playContext: MusicPlayContext = activeContext) {
        lastSettings = settings
        activeContext = playContext
        val volume = if (settings.lowMemoryAudioMode) settings.musicVolume * 0.85f else settings.musicVolume
        runCatching { mediaPlayer?.setVolume(volume, volume) }

        val allowedInContext = when (playContext) {
            MusicPlayContext.MENU -> settings.musicInMenu
            MusicPlayContext.GAME -> settings.musicInGame
            MusicPlayContext.TRAINING -> settings.musicInTraining
        }
        if (!settings.musicEnabled || !allowedInContext) {
            if (_uiState.value.isPlaying) pause()
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filterSource = filter) }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(shuffle = !it.shuffle) }
    }

    fun cycleRepeatMode() {
        _uiState.update { st ->
            val next = when (st.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            st.copy(repeatMode = next)
        }
    }

    fun playTrack(trackId: String? = null) {
        val st = _uiState.value
        val target = if (trackId != null) {
            st.tracks.firstOrNull { it.id == trackId }
        } else {
            st.currentTrack
        } ?: return

        stopPlaybackEngines()
        _uiState.update {
            it.copy(
                currentTrackId = target.id,
                isPlaying = true,
                positionMs = 0L,
                durationMs = target.durationMs.coerceAtLeast(1000L),
            )
        }

        if (target.isBuiltIn) {
            startBuiltInSynth(target)
        } else {
            startMediaPlayer(target)
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            playTrack(_uiState.value.currentTrackId)
        }
    }

    fun pause() {
        stopPlaybackEngines()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun nextTrack() {
        val st = _uiState.value
        val list = filteredTracks(st)
        if (list.isEmpty()) return
        val next = if (st.shuffle && list.size > 1) {
            list.filter { it.id != st.currentTrackId }.randomOrNull() ?: list.first()
        } else {
            val idx = list.indexOfFirst { it.id == st.currentTrackId }
            if (idx < 0 || idx + 1 >= list.size) list.first() else list[idx + 1]
        }
        playTrack(next.id)
    }

    fun previousTrack() {
        val st = _uiState.value
        val list = filteredTracks(st)
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == st.currentTrackId }
        val prev = if (idx <= 0) list.last() else list[idx - 1]
        playTrack(prev.id)
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, _uiState.value.durationMs)
        _uiState.update { it.copy(positionMs = clamped) }
        runCatching { mediaPlayer?.seekTo(clamped.toInt()) }
    }

    private fun filteredTracks(st: MusicPlayerUiState): List<MusicTrackEntity> = when (st.filterSource) {
        "Built-in" -> st.tracks.filter { it.isBuiltIn }
        "My Music" -> st.tracks.filter { !it.isBuiltIn }
        else -> st.tracks
    }

    private fun startMediaPlayer(track: MusicTrackEntity) {
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(appContext, Uri.parse(track.uriString))
                val vol = lastSettings.musicVolume.coerceIn(0f, 1f)
                setVolume(vol, vol)
                setOnPreparedListener { player ->
                    val dur = player.duration.toLong().coerceAtLeast(1000L)
                    _uiState.update { it.copy(durationMs = dur) }
                    player.start()
                    startProgressTicker()
                }
                setOnCompletionListener {
                    handleTrackCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    _uiState.update { it.copy(isPlaying = false) }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure {
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun startBuiltInSynth(track: MusicTrackEntity) {
        val baseFreq = when {
            track.id.contains("endgame") -> 220.0
            track.id.contains("tactics") -> 261.63
            else -> 196.0
        }
        val fifthFreq = baseFreq * 1.5
        val sampleRate = if (lastSettings.lowMemoryAudioMode) 11025 else 22050
        synthJob = scope.launch(Dispatchers.IO) {
            var trackOut: AudioTrack? = null
            runCatching {
                val bufSamples = sampleRate / 2
                val pcm = ShortArray(bufSamples)
                val out = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufSamples * 4)
                    .build()
                trackOut = out
                out.play()
                var phase = 0L
                while (isActive) {
                    val vol = (lastSettings.musicVolume * 0.14f).coerceIn(0f, 0.25f)
                    for (i in 0 until bufSamples) {
                        val t = (phase + i).toDouble() / sampleRate
                        val s = (sin(2.0 * PI * baseFreq * t) + 0.55 * sin(2.0 * PI * fifthFreq * t)) * 0.5
                        pcm[i] = (s * Short.MAX_VALUE * vol).toInt().toShort()
                    }
                    phase += bufSamples
                    out.write(pcm, 0, bufSamples)
                }
            }
            runCatching {
                trackOut?.stop()
                trackOut?.release()
            }
        }
        startProgressTicker()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _uiState.value.isPlaying) {
                delay(500L)
                val mpPos = runCatching { mediaPlayer?.takeIf { it.isPlaying }?.currentPosition?.toLong() }.getOrNull()
                _uiState.update { st ->
                    val nextPos = mpPos ?: (st.positionMs + 500L)
                    if (nextPos >= st.durationMs && mpPos == null) {
                        scope.launch { handleTrackCompleted() }
                        st
                    } else {
                        st.copy(positionMs = nextPos.coerceAtMost(st.durationMs))
                    }
                }
            }
        }
    }

    private fun handleTrackCompleted() {
        val st = _uiState.value
        when (st.repeatMode) {
            RepeatMode.ONE -> playTrack(st.currentTrackId)
            RepeatMode.ALL -> nextTrack()
            RepeatMode.OFF -> {
                val list = filteredTracks(st)
                val idx = list.indexOfFirst { it.id == st.currentTrackId }
                if (idx >= 0 && idx + 1 < list.size) {
                    playTrack(list[idx + 1].id)
                } else {
                    pause()
                }
            }
        }
    }

    private fun stopPlaybackEngines() {
        progressJob?.cancel()
        progressJob = null
        synthJob?.cancel()
        synthJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    fun release() {
        stopPlaybackEngines()
    }
}
