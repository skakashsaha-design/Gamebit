package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CollectionEntity
import com.example.data.local.LocalChessRepository
import com.example.data.local.LocalGameEntity
import com.example.data.local.LocalStudyEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArchiveTab(
    savedGames: List<LocalGameEntity>,
    onLoadGame: (LocalGameEntity) -> Unit,
    onToggleFavorite: (LocalGameEntity) -> Unit,
    onDeleteGame: (String) -> Unit,
    onImportPgn: (String) -> Boolean,
    onLoadFen: (String) -> Boolean,
    onShareText: (String, String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importFeedback by remember { mutableStateOf<String?>(null) }

    val filtered = remember(savedGames, searchQuery, favoritesOnly) {
        savedGames.filter { g ->
            val matchesFav = !favoritesOnly || g.favorite
            val matchesQuery = searchQuery.isBlank() ||
                g.whiteName.contains(searchQuery, ignoreCase = true) ||
                g.blackName.contains(searchQuery, ignoreCase = true) ||
                g.openingName.contains(searchQuery, ignoreCase = true) ||
                g.openingEco.contains(searchQuery, ignoreCase = true) ||
                g.variant.contains(searchQuery, ignoreCase = true)
            matchesFav && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("PGN & FEN Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it; importFeedback = null },
                label = { Text("Paste PGN or FEN") },
                modifier = Modifier.fillMaxWidth().testTag("import_pgn_fen_input"),
                maxLines = 4,
            )
            importFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(
                    onClick = {
                        val ok = onImportPgn(importText)
                        if (!ok) importFeedback = "Could not parse PGN."
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Load PGN") }
                FilledTonalButton(
                    onClick = {
                        val ok = onLoadFen(importText)
                        if (!ok) importFeedback = "Invalid FEN string."
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Load FEN") }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Archive (player, ECO, opening, variant)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Favorites Only") },
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    "No matching archived games.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(filtered, key = { it.id }) { game ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLoadGame(game) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${game.whiteName} vs ${game.blackName} (${game.result})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${game.variant} • ${game.timeControl} • ${game.openingEco} ${game.openingName}".trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(game) }) {
                            Icon(
                                if (game.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = { onShareText("ChessHub PGN", game.pgn) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share PGN")
                        }
                        IconButton(onClick = { onDeleteGame(game.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete game")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudiesAndCollectionsTab(
    studies: List<LocalStudyEntity>,
    collections: List<CollectionEntity>,
    isOnline: Boolean,
    onLoadStudyPgn: (String) -> Unit,
    onPublishCloud: (String, String, String) -> Unit,
    onCreateCollection: (String, String) -> Unit,
) {
    var colName by remember { mutableStateOf("") }
    var colDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Saved Studies & Repertoires", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (studies.isEmpty()) {
            item {
                Text(
                    "Save any analyzed position from the Analysis tab to create a study.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(studies, key = { it.id }) { study ->
                val chapters = remember(study.chaptersJson) { LocalChessRepository.decodeChapters(study.chaptersJson) }
                val firstPgn = chapters.firstOrNull()?.pgn.orEmpty()
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (firstPgn.isNotBlank()) onLoadStudyPgn(firstPgn)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(study.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${study.tags} • ${chapters.size} chapter(s) • ${study.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isOnline && firstPgn.isNotBlank()) {
                            OutlinedButton(onClick = { onPublishCloud(study.title, study.description, firstPgn) }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Publish")
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text("Custom Game Collections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = colName,
                    onValueChange = { colName = it },
                    label = { Text("Collection Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = {
                    if (colName.isNotBlank()) {
                        onCreateCollection(colName.trim(), colDesc.trim())
                        colName = ""
                        colDesc = ""
                    }
                }) { Text("Create") }
            }
        }
        items(collections, key = { it.id }) { col ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(col.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (col.description.isNotBlank()) {
                            Text(col.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
