package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.MusicPlayerManager
import com.example.audio.MusicPlayerUiState
import com.example.audio.RepeatMode
import com.example.data.local.AppSettings
import com.example.data.local.CachedNewsEntity
import com.example.data.local.MusicTrackEntity
import com.example.data.remote.ExternalChessApis
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MusicPlayerScreen(
    musicState: MusicPlayerUiState,
    musicManager: MusicPlayerManager,
    settings: AppSettings,
    onAddPickedTracks: (List<MusicTrackEntity>) -> Unit,
    onDeleteTrack: (String) -> Unit,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicScopeChange: (String, Boolean) -> Unit,
    onLowMemAudioChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    BackHandler { onBack() }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val tracks = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio Track"
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = c.getString(idx) ?: displayName
                        }
                    }
                }
            }
            MusicTrackEntity(
                id = "usr_${UUID.randomUUID().toString().take(12)}",
                title = displayName.substringBeforeLast('.').ifBlank { displayName },
                artist = "Device Audio (SAF)",
                uriString = uri.toString(),
                playlistName = "My Focus Playlist",
                isBuiltIn = false,
            )
        }
        if (tracks.isNotEmpty()) {
            onAddPickedTracks(tracks)
            musicManager.playTrack(tracks.first().id)
        }
    }

    val filtered = remember(musicState.tracks, musicState.filterSource) {
        when (musicState.filterSource) {
            "Built-in" -> musicState.tracks.filter { it.isBuiltIn }
            "My Music" -> musicState.tracks.filter { !it.isBuiltIn }
            else -> musicState.tracks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Music Player") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("music_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.testTag("add_device_audio_btn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add local audio files")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Now Playing Card
            item {
                val track = musicState.currentTrack
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track?.title ?: "No track selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${track?.artist ?: "-"} • ${track?.playlistName ?: "Focus"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        val maxDur = musicState.durationMs.coerceAtLeast(1000L).toFloat()
                        Slider(
                            value = musicState.positionMs.toFloat().coerceIn(0f, maxDur),
                            onValueChange = { musicManager.seekTo(it.toLong()) },
                            valueRange = 0f..maxDur,
                            modifier = Modifier.testTag("music_seek_slider"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(formatTrackMs(musicState.positionMs), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            Text(formatTrackMs(musicState.durationMs), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { musicManager.toggleShuffle() },
                                modifier = Modifier.testTag("music_shuffle_btn"),
                            ) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (musicState.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { musicManager.previousTrack() }, modifier = Modifier.testTag("music_prev_btn")) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (!settings.musicEnabled) onMusicEnabledChange(true)
                                    musicManager.togglePlayPause()
                                },
                                modifier = Modifier.testTag("music_play_pause_btn"),
                            ) {
                                Icon(
                                    if (musicState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (musicState.isPlaying) "Pause" else "Play")
                            }
                            IconButton(onClick = { musicManager.nextTrack() }, modifier = Modifier.testTag("music_next_btn")) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                            }
                            IconButton(onClick = { musicManager.cycleRepeatMode() }, modifier = Modifier.testTag("music_repeat_btn")) {
                                Icon(
                                    imageVector = if (musicState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat: ${musicState.repeatMode.name}",
                                    tint = if (musicState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Music Context & Volume Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Audio Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Enable Background Music", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = settings.musicEnabled, onCheckedChange = onMusicEnabledChange)
                        }
                        Text("Music Volume: ${(settings.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.musicVolume,
                            onValueChange = onMusicVolumeChange,
                            valueRange = 0f..1f,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.musicInMenu,
                                onClick = { onMusicScopeChange("menu", !settings.musicInMenu) },
                                label = { Text("In Menus") },
                            )
                            FilterChip(
                                selected = settings.musicInGame,
                                onClick = { onMusicScopeChange("game", !settings.musicInGame) },
                                label = { Text("During Games") },
                            )
                            FilterChip(
                                selected = settings.musicInTraining,
                                onClick = { onMusicScopeChange("training", !settings.musicInTraining) },
                                label = { Text("During Training") },
                            )
                            FilterChip(
                                selected = settings.lowMemoryAudioMode,
                                onClick = { onLowMemAudioChange(!settings.lowMemoryAudioMode) },
                                label = { Text("Low-RAM Audio") },
                            )
                        }
                    }
                }
            }

            // Library Header + Add Device Music
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Track Library (${filtered.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Device Audio")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf("All", "Built-in", "My Music").forEach { f ->
                        FilterChip(
                            selected = musicState.filterSource == f,
                            onClick = { musicManager.setFilter(f) },
                            label = { Text(f) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No custom tracks added yet", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Tap 'Pick Device Audio' to select MP3, M4A, OGG, or WAV files securely via Android's document picker — no broad storage permissions required.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val isSelected = item.id == musicState.currentTrackId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!settings.musicEnabled) onMusicEnabledChange(true)
                                musicManager.playTrack(item.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (isSelected && musicState.isPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.artist} • ${if (item.isBuiltIn) "Built-in Synth" else "Local File"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!item.isBuiltIn) {
                                IconButton(onClick = { onDeleteTrack(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove track")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChessNewsScreen(
    cachedNews: List<CachedNewsEntity>,
    externalApis: ExternalChessApis,
    onSaveCachedNews: (List<CachedNewsEntity>) -> Unit,
    onClearNewsCache: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var offlineNotice by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    fun refreshNews() {
        scope.launch {
            isLoading = true
            errorMessage = null
            offlineNotice = false
            externalApis.fetchChessNews().fold(
                onSuccess = { items ->
                    val entities = items.map { item ->
                        CachedNewsEntity(
                            id = item.id,
                            title = item.title,
                            summary = item.summary,
                            url = item.url,
                            sourceName = item.source,
                            publishedAt = item.publishedAt,
                            category = "Chess & Tournament News",
                        )
                    }
                    if (entities.isNotEmpty()) onSaveCachedNews(entities)
                    isLoading = false
                },
                onFailure = { err ->
                    isLoading = false
                    if (cachedNews.isNotEmpty()) {
                        offlineNotice = true
                    }
                    errorMessage = err.localizedMessage ?: "Unable to fetch news feed."
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshNews()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess News") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("news_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshNews() }, modifier = Modifier.testTag("news_refresh_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh news")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Official Chess Headlines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Source: Lichess Official Blog (Atom RSS) • Cached offline when fetched",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (cachedNews.isNotEmpty()) {
                        TextButton(onClick = onClearNewsCache) { Text("Clear Cache") }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Fetching latest chess & tournament articles...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (offlineNotice || (errorMessage != null && cachedNews.isNotEmpty())) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Offline cached articles displayed (network unavailable).",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { refreshNews() }) { Text("Retry") }
                        }
                    }
                }
            }

            if (cachedNews.isEmpty() && !isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = errorMessage ?: "No cached articles available yet.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { refreshNews() }) { Text("Retry Fetch") }
                        }
                    }
                }
            } else {
                items(cachedNews, key = { it.id }) { article ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(article.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(article.publishedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(article.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                article.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Read full article", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatTrackMs(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val mins = total / 60L
    val secs = total % 60L
    return String.format("%d:%02d", mins, secs)
}
