package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.chess.CountryCatalog
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.local.AnimationMode
import com.example.data.local.AppSettings
import com.example.data.local.LocalGameEntity
import com.example.data.local.PerformanceMode
import com.example.data.local.RatingCategoryStats
import com.example.data.local.VoiceMode
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.PublicProfileDoc
import com.example.ui.components.GamebitDialog
import com.example.ui.components.PieceMiniIcon
import com.example.ui.components.PlayerAvatarCatalog
import com.example.ui.components.PlayerAvatarView
import com.example.ui.theme.ChessBoardThemes
import com.example.ui.theme.ChessPieceStyles
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAndLookupTab(
    isOnline: Boolean,
    settings: AppSettings,
    myProfile: PublicProfileDoc?,
    externalSummary: ExternalPlayerSummary?,
    savedGames: List<LocalGameEntity> = emptyList(),
    onOpenSavedGame: (LocalGameEntity) -> Unit = {},
    onRequestSignIn: () -> Unit,
    onUpdateProfile: (String, String, String, String, Boolean) -> Unit,
    onUpdateLocalIdentity: (String, String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onLookupExternalPlayer: (String, String) -> Unit,
    onImportExternalPgn: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {},
) {
    var username by remember(myProfile?.username, settings.localDisplayName) {
        mutableStateOf(myProfile?.username ?: settings.localDisplayName)
    }
    var bio by remember(myProfile?.bio, settings.localBio) {
        mutableStateOf(myProfile?.bio ?: settings.localBio)
    }
    var countryIso by remember(myProfile?.countryIso, settings.countryIso) {
        mutableStateOf(myProfile?.countryIso ?: settings.countryIso)
    }
    var fideFed by remember(myProfile?.fideFederation, settings.fideFederation) {
        mutableStateOf(myProfile?.fideFederation ?: settings.fideFederation)
    }
    var publicVisible by remember(myProfile?.publicVisible) { mutableStateOf(myProfile?.publicVisible ?: true) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showFidePicker by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    var extPlatform by remember { mutableStateOf("Lichess") }
    var extUsername by remember { mutableStateOf("") }

    val selectedAvatarOpt = PlayerAvatarCatalog.forId(settings.localAvatarId)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero Player Profile Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_hero_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerAvatarView(
                            avatarId = selectedAvatarOpt.id,
                            size = 64.dp,
                            pieceStyleId = settings.pieceStyleId,
                            cornerRadius = 16.dp,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = username.ifBlank { "Gamebit Player" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = CountryCatalog.flagForIso(countryIso),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Text(
                                text = "FIDE: $fideFed • ${if (isOnline) "Cloud Synced" else "Local Offline Profile"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (bio.isNotBlank()) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileStatTile(
                            label = "Local Games",
                            value = savedGames.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        ProfileStatTile(
                            label = "Puzzle ELO",
                            value = settings.localPuzzleRating.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        ProfileStatTile(
                            label = "Best Streak",
                            value = settings.puzzleBestStreak.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Edit Identity & Avatar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose Player Crest (${selectedAvatarOpt.label})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlayerAvatarCatalog.ALL.forEach { opt ->
                            val selected = settings.localAvatarId == opt.id
                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onAvatarSelected(opt.id) }
                                    .testTag("avatar_option_${opt.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = if (selected) BorderStroke(2.dp, Color(opt.accentHex)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    PlayerAvatarView(
                                        avatarId = opt.id,
                                        size = 42.dp,
                                        pieceStyleId = settings.pieceStyleId,
                                        cornerRadius = 11.dp,
                                    )
                                    Text(
                                        text = opt.label.substringBefore(' '),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${CountryCatalog.flagForIso(countryIso)} Country: $countryIso  •  FIDE Federation: $fideFed",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCountryPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("Country: ${CountryCatalog.flagForIso(countryIso)} $countryIso")
                        }
                        OutlinedButton(onClick = { showFidePicker = true }, modifier = Modifier.weight(1f)) {
                            Text("FIDE: $fideFed")
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / Chess Motto") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )

                    if (isOnline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Public Leaderboard Profile")
                            Switch(checked = publicVisible, onCheckedChange = { publicVisible = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdateLocalIdentity(username, bio)
                                    onUpdateProfile(username, bio, countryIso, fideFed, publicVisible)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Save Profile") }
                            OutlinedButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_btn")) {
                                Text(stringResource(R.string.sign_out))
                            }
                        }
                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("delete_account_btn"),
                        ) {
                            Text("Delete Online Account")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onUpdateLocalIdentity(username, bio) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Save Local Profile") }
                            Button(onClick = onRequestSignIn, modifier = Modifier.weight(1f)) {
                                Text("Online Sign-In")
                            }
                        }
                    }
                }
            }
        }

        // Ratings & Performance Breakdown (Realistic [200..3200] stats + Sparkline trends)
        item {
            val gamesPlayed = (myProfile?.gamesPlayed ?: 0L).toInt()
            val wins = (myProfile?.wins ?: 0L).toInt()
            val draws = (myProfile?.draws ?: 0L).toInt()
            val losses = (myProfile?.losses ?: 0L).toInt()
            val winPct = if (gamesPlayed > 0) ((wins * 100f) / gamesPlayed).roundToInt() else 0
            val drawPct = if (gamesPlayed > 0) ((draws * 100f) / gamesPlayed).roundToInt() else 0
            val lossPct = if (gamesPlayed > 0) (100 - winPct - drawPct).coerceAtLeast(0) else 0

            val categories = listOf(
                Triple("Bullet", settings.localBulletStats, myProfile?.bulletRating?.toInt()),
                Triple("Blitz", settings.localBlitzStats, myProfile?.blitzRating?.toInt()),
                Triple("Rapid", settings.localRapidStats, myProfile?.rapidRating?.toInt()),
                Triple("Classical", settings.localClassicalStats, myProfile?.classicalRating?.toInt()),
                Triple("Puzzle", settings.localPuzzleStats, null),
            )

            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_ratings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ratings & Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    categories.forEach { (label, localStats, onlineRating) ->
                        RatingCategoryRow(
                            label = label,
                            stats = localStats,
                            onlineRating = onlineRating,
                            isPuzzle = label == "Puzzle",
                        )
                    }
                    if (gamesPlayed > 0) {
                        Text(
                            "Online Record: $gamesPlayed games ($wins W / $draws D / $losses L) — $winPct% Win / $drawPct% Draw / $lossPct% Loss",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            "Best Puzzle Streak: ${settings.puzzleBestStreak} • Best Storm: ${settings.puzzleBestStorm}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        // Local Match History & Results Summary
        item {
            val localWins = savedGames.count {
                (it.whiteName == "You" && it.result == "1-0") || (it.blackName == "You" && it.result == "0-1")
            }
            val localDraws = savedGames.count { it.result == "1/2-1/2" }
            val localLosses = (savedGames.size - localWins - localDraws).coerceAtLeast(0)

            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_local_history_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Local Game History (${savedGames.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (savedGames.isEmpty()) {
                        Text(
                            "No finished local games saved yet. Complete a game in Play to build your personal match history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Summary: $localWins Wins • $localDraws Draws • $localLosses Losses/OTB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        savedGames.take(3).forEach { game ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${game.whiteName} vs ${game.blackName} (${game.result})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${game.variant} • ${game.timeControl} • ${game.openingName.ifBlank { game.resultReason }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = { onOpenSavedGame(game) }) {
                                        Text("Analyze")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Badges & Achievements (computed strictly from real user activity)
        item {
            val badges = remember(savedGames, myProfile, settings) {
                computeChessHubAchievements(savedGames, myProfile, settings)
            }
            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_achievements_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Titles, Badges & Achievements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    badges.forEach { badge ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(badge.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (badge.unlocked) {
                                Icon(Icons.Default.Verified, contentDescription = "Unlocked", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Official Public External Player Lookup (Lichess & Chess.com)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Official Public Profile Lookup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Read-only lookup via official public Lichess & Chess.com APIs to inspect ratings and import recent PGNs for analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lichess", "Chess.com").forEach { p ->
                            FilterChip(selected = extPlatform == p, onClick = { extPlatform = p }, label = { Text(p) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = extUsername,
                            onValueChange = { extUsername = it },
                            label = { Text("$extPlatform Username") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(onClick = { onLookupExternalPlayer(extPlatform, extUsername) }) { Text("Fetch") }
                    }

                    externalSummary?.let { summary ->
                        Text("${summary.platform}: ${summary.username}", fontWeight = FontWeight.Bold)
                        Text("Ratings: ${summary.ratingSummary}", style = MaterialTheme.typography.bodySmall)
                        summary.recentPgns.take(3).forEachIndexed { i, pgn ->
                            FilledTonalButton(onClick = { onImportExternalPgn(pgn) }) {
                                Text("Analyze Recent Game #${i + 1}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCountryPicker) {
        SearchableCountryDialog(
            onDismiss = { showCountryPicker = false },
            onSelect = { info ->
                countryIso = info.isoCode
                showCountryPicker = false
                if (isOnline) onUpdateProfile(username, bio, countryIso, fideFed, publicVisible)
            },
        )
    }

    if (showFidePicker) {
        SearchableFideDialog(
            onDismiss = { showFidePicker = false },
            onSelect = { code ->
                fideFed = code
                showFidePicker = false
                if (isOnline) onUpdateProfile(username, bio, countryIso, fideFed, publicVisible)
            },
        )
    }

    if (showDeleteAccountDialog) {
        GamebitDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = "Delete Online Account?",
            subtitle = "This permanently removes your Gamebit server profile and signs you out.",
            actions = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_account_btn"),
                ) { Text("Delete Account") }
            },
        ) {
            Text(
                "Your local Room archive, custom puzzles, studies, and offline ratings remain safely stored on this device.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RatingCategoryRow(
    label: String,
    stats: RatingCategoryStats,
    onlineRating: Int?,
    isPuzzle: Boolean,
) {
    val displayedRating = (onlineRating ?: stats.rating).coerceIn(200, 3200)
    val peak = maxOf(stats.peakRating, displayedRating).coerceIn(200, 3200)
    val deltaStr = when {
        stats.lastDelta > 0 -> "+${stats.lastDelta}"
        stats.lastDelta < 0 -> "${stats.lastDelta}"
        else -> "0"
    }
    val deltaColor = when {
        stats.lastDelta > 0 -> Color(0xFF10B981)
        stats.lastDelta < 0 -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val lineColor = if (isPuzzle) Color(0xFFF59E0B) else Color(0xFF10B981)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Peak $peak • ${stats.gamesPlayed} games",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Canvas(modifier = Modifier.width(68.dp).height(24.dp)) {
                val points = if (stats.history.size >= 2) stats.history else listOf(displayedRating, displayedRating)
                val minP = (points.minOrNull() ?: 1200) - 20
                val maxP = (points.maxOrNull() ?: 1200) + 20
                val span = (maxP - minP).coerceAtLeast(40).toFloat()
                val path = Path()
                points.forEachIndexed { idx, v ->
                    val x = (idx.toFloat() / (points.size - 1)) * size.width
                    val y = size.height - ((v - minP) / span) * size.height
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$displayedRating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(deltaStr, style = MaterialTheme.typography.labelSmall, color = deltaColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchableCountryDialog(
    onDismiss: () -> Unit,
    onSelect: (com.example.chess.CountryInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        CountryCatalog.ALL_COUNTRIES.filter {
            query.isBlank() || it.name.contains(query, true) || it.isoCode.contains(query, true)
        }
    }
    GamebitDialog(
        onDismissRequest = onDismiss,
        title = "Select Country",
        actions = { TextButton(onClick = onDismiss) { Text("Close") } },
    ) {
        Column(modifier = Modifier.height(340.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search country...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtered, key = { it.isoCode }) { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(c) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${c.flagEmoji} ${c.name}")
                        Text(c.isoCode, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchableFideDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        CountryCatalog.FIDE_FEDERATIONS.filter { query.isBlank() || it.contains(query, true) }
    }
    GamebitDialog(
        onDismissRequest = onDismiss,
        title = "Select FIDE Federation",
        actions = { TextButton(onClick = onDismiss) { Text("Close") } },
    ) {
        Column(modifier = Modifier.height(320.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search FIDE code (e.g., BAN, FID, ENG)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtered, key = { it }) { code ->
                    Text(
                        text = code,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(code) }.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndAboutScreen(
    settings: AppSettings,
    isLowRamDevice: Boolean,
    onBack: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onBoardThemeChange: (String) -> Unit,
    onPieceStyleChange: (String) -> Unit,
    onShowCoordsChange: (Boolean) -> Unit,
    onShowLegalChange: (Boolean) -> Unit,
    onShowLastMoveChange: (Boolean) -> Unit,
    onMoveAnimationChange: (Boolean) -> Unit,
    onReducedAnimationsChange: (Boolean) -> Unit,
    onAnimationSpeedChange: (Float) -> Unit,
    onLowMemoryChange: (Boolean) -> Unit,
    onAutoQueenChange: (Boolean) -> Unit,
    onConfirmMoveChange: (Boolean) -> Unit,
    onPremoveChange: (Boolean) -> Unit,
    onCoordNotationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onSfxFlagChange: (String, Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBotVoiceChange: (Boolean) -> Unit,
    onTrainerVoiceChange: (Boolean) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicScopeChange: (String, Boolean) -> Unit,
    onLowMemAudioChange: (Boolean) -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onEvalBarChange: (Boolean) -> Unit,
    onBestMoveArrowChange: (Boolean) -> Unit,
    onThreatArrowChange: (Boolean) -> Unit,
    onAllowBoardArrowsChange: (Boolean) -> Unit,
    onEngineDepthChange: (Int) -> Unit,
    onEngineMultiPvChange: (Int) -> Unit,
    onOpeningBookChange: (Boolean) -> Unit,
    onOpeningRecogChange: (Boolean) -> Unit,
    onGambitRecogChange: (Boolean) -> Unit,
    onEndgameRecogChange: (Boolean) -> Unit,
    onAllowChatChange: (Boolean) -> Unit,
    onSocialFeedChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onLanguageChange: (String) -> Unit,
    onClearNewsCache: () -> Unit,
    onAnimationModeChange: (AnimationMode) -> Unit = {},
    onPerformanceModeChange: (PerformanceMode) -> Unit = {},
    onVoiceModeChange: (VoiceMode) -> Unit = {},
    onVoiceSpeedChange: (Float) -> Unit = {},
    onMoveFeedbackChange: (Boolean) -> Unit = {},
) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Appearance, Board & Piece Styles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Dark Theme", settings.darkTheme, onDarkThemeChange) }
            item { SettingSwitchRow("High Contrast Mode", settings.highContrast, onHighContrastChange) }
            item {
                Text("Board Theme", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChessBoardThemes.forEach { t ->
                        FilterChip(
                            selected = settings.boardThemeId == t.id,
                            onClick = { onBoardThemeChange(t.id) },
                            label = { Text(t.displayName) },
                        )
                    }
                }
            }
            item {
                Text("Piece Style", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChessPieceStyles.forEach { ps ->
                        FilterChip(
                            selected = settings.pieceStyleId == ps.id,
                            onClick = { onPieceStyleChange(ps.id) },
                            leadingIcon = {
                                PieceMiniIcon(
                                    piece = Piece(PieceType.KNIGHT, PieceColor.WHITE),
                                    modifier = Modifier.size(18.dp),
                                    pieceStyleId = ps.id,
                                )
                            },
                            label = { Text(ps.displayName) },
                        )
                    }
                }
            }
            item {
                Text("Language / ভাষা", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings.languageCode == "en", onClick = { onLanguageChange("en") }, label = { Text("English") })
                    FilterChip(selected = settings.languageCode == "bn", onClick = { onLanguageChange("bn") }, label = { Text("বাংলা (Bengali)") })
                }
            }
            item {
                Text("Font Scale: ${String.format("%.2fx", settings.fontScale)}", style = MaterialTheme.typography.labelLarge)
                Slider(value = settings.fontScale, onValueChange = onFontScaleChange, valueRange = 0.85f..1.35f)
            }

            item { Text("Board Visuals, Motion & Gameplay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Show Coordinates", settings.showCoordinates, onShowCoordsChange) }
            item { SettingSwitchRow("Show Legal Move Indicators", settings.showLegalMoves, onShowLegalChange) }
            item { SettingSwitchRow("Highlight Last Move", settings.showLastMove, onShowLastMoveChange) }
            item { SettingSwitchRow("Piece Slide Move Animation", settings.moveAnimation, onMoveAnimationChange) }
            item { SettingSwitchRow("Reduced Animations (Accessibility)", settings.reducedAnimations, onReducedAnimationsChange) }
            item {
                Text("Animation Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.animationMode == mode,
                            onClick = { onAnimationModeChange(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("anim_mode_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
            item {
                Text("Animation Speed: ${String.format(Locale.US, "%.1fx", settings.animationSpeed)}")
                Slider(value = settings.animationSpeed, onValueChange = onAnimationSpeedChange, valueRange = 0.5f..2.0f)
            }
            item { SettingSwitchRow("Live Move Quality Feedback", settings.moveFeedbackEnabled, onMoveFeedbackChange) }
            item { SettingSwitchRow("Auto-Queen on Promotion", settings.autoQueen, onAutoQueenChange) }
            item { SettingSwitchRow("Confirm Moves Before Sending", settings.confirmMoves, onConfirmMoveChange) }
            item { SettingSwitchRow("Enable Premoves", settings.premoveEnabled, onPremoveChange) }
            item { SettingSwitchRow("Coordinate Notation Only", settings.coordinateNotationOnly, onCoordNotationChange) }

            item { Text("Engine, Analysis & Recognition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Show Evaluation Bar", settings.showEvalBar, onEvalBarChange) }
            item { SettingSwitchRow("Show Best-Move Arrow", settings.showBestMoveArrow, onBestMoveArrowChange) }
            item { SettingSwitchRow("Show Opponent Threat Arrow", settings.showThreatArrow, onThreatArrowChange) }
            item { SettingSwitchRow("Allow Manual Arrows & Circles", settings.allowBoardArrows, onAllowBoardArrowsChange) }
            item {
                Text("Analysis Engine Depth: ${settings.engineDepth}")
                Slider(
                    value = settings.engineDepth.toFloat(),
                    onValueChange = { onEngineDepthChange(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }
            item {
                Text("Multi-PV Candidate Lines: ${settings.engineMultiPv}")
                Slider(
                    value = settings.engineMultiPv.toFloat(),
                    onValueChange = { onEngineMultiPvChange(it.roundToInt()) },
                    valueRange = 1f..3f,
                    steps = 1,
                )
            }
            item { SettingSwitchRow("Built-in Opening Book", settings.openingBookEnabled, onOpeningBookChange) }
            item { SettingSwitchRow("Real-Time Opening Recognition", settings.openingRecognitionEnabled, onOpeningRecogChange) }
            item { SettingSwitchRow("Gambit Recognition Banner", settings.gambitRecognitionEnabled, onGambitRecogChange) }
            item { SettingSwitchRow("Endgame Pattern Classification", settings.endgameRecognitionEnabled, onEndgameRecogChange) }

            item { Text("Audio, SFX, Voice & Focus Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Master Sound Effects", settings.soundEnabled, onSoundChange) }
            item {
                Text("SFX Volume: ${(settings.soundVolume * 100).toInt()}%")
                Slider(value = settings.soundVolume, onValueChange = onSoundVolumeChange, valueRange = 0f..1f)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("move", "Move", settings.sfxMove),
                        Triple("capture", "Capture", settings.sfxCapture),
                        Triple("check", "Check", settings.sfxCheck),
                        Triple("checkmate", "Mate/End", settings.sfxCheckmate),
                        Triple("promotion", "Promotion", settings.sfxPromotion),
                        Triple("feedback", "Puzzle SFX", settings.sfxFeedback),
                        Triple("button", "UI Click", settings.sfxButton),
                        Triple("book", "Book Move", settings.sfxBookMove),
                    ).forEach { (key, label, active) ->
                        FilterChip(
                            selected = active,
                            onClick = { onSfxFlagChange(key, !active) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item { SettingSwitchRow("Haptic Vibration Feedback", settings.hapticsEnabled, onHapticsChange) }
            item {
                Text("Voice Coaching Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoiceMode.entries.forEach { vm ->
                        FilterChip(
                            selected = settings.voiceMode == vm,
                            onClick = { onVoiceModeChange(vm) },
                            label = { Text(vm.label) },
                            modifier = Modifier.testTag("voice_mode_${vm.name.lowercase()}"),
                        )
                    }
                }
            }
            item { SettingSwitchRow("Bot Personality Voice (TTS)", settings.botVoiceEnabled, onBotVoiceChange) }
            item { SettingSwitchRow("Coach / Trainer Hint Voice (TTS)", settings.trainerVoiceEnabled, onTrainerVoiceChange) }
            item {
                Text("Voice Volume: ${(settings.voiceVolume * 100).toInt()}%")
                Slider(value = settings.voiceVolume, onValueChange = onVoiceVolumeChange, valueRange = 0f..1f)
            }
            item {
                Text("Voice Speed: ${String.format(Locale.US, "%.2fx", settings.voiceSpeed)}")
                Slider(value = settings.voiceSpeed, onValueChange = onVoiceSpeedChange, valueRange = 0.75f..1.50f)
            }
            item { SettingSwitchRow("Ambient Focus Music", settings.musicEnabled, onMusicEnabledChange) }
            item {
                Text("Music Volume: ${(settings.musicVolume * 100).toInt()}%")
                Slider(value = settings.musicVolume, onValueChange = onMusicVolumeChange, valueRange = 0f..1f)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = settings.musicInMenu, onClick = { onMusicScopeChange("menu", !settings.musicInMenu) }, label = { Text("In Menu") })
                    FilterChip(selected = settings.musicInGame, onClick = { onMusicScopeChange("game", !settings.musicInGame) }, label = { Text("In Game") })
                    FilterChip(selected = settings.musicInTraining, onClick = { onMusicScopeChange("training", !settings.musicInTraining) }, label = { Text("Training") })
                }
                FilledTonalButton(onClick = onOpenMusicPlayer, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Music Player & Device Audio Picker")
                }
            }

            item { Text("Social, Privacy & Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Allow Online Game Chat", settings.allowChatInGames, onAllowChatChange) }
            item { SettingSwitchRow("Show Community Social Feed", settings.showSocialFeed, onSocialFeedChange) }
            item { SettingSwitchRow("Event & Challenge Notifications", settings.notificationsEnabled, onNotificationsChange) }
            item {
                Text("Performance Preset", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PerformanceMode.entries.forEach { pm ->
                        FilterChip(
                            selected = settings.performanceMode == pm,
                            onClick = { onPerformanceModeChange(pm) },
                            label = { Text(pm.label) },
                            modifier = Modifier.testTag("perf_mode_${pm.name.lowercase()}"),
                        )
                    }
                }
                Text(
                    text = settings.performanceMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SettingSwitchRow("Low-Memory Audio Mode", settings.lowMemoryAudioMode, onLowMemAudioChange) }
            item {
                SettingSwitchRow(
                    title = if (isLowRamDevice) "Low-Memory Mode (Recommended)" else "Low-Memory Mode (Android Go / Low RAM)",
                    checked = settings.lowMemoryMode,
                    onCheckedChange = onLowMemoryChange,
                )
            }
            item {
                OutlinedButton(onClick = onClearNewsCache, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Cached Chess News Articles")
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About Gamebit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                        Text("Package: ${BuildConfig.APPLICATION_ID}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Gamebit combines pure-Kotlin offline chess intelligence (ChessHub Kotlin UCI Engine 2.0, 9 variants, 18 bots, Mistake Breakdown, Puzzles & Opening Drills) with modular REST + WebSocket online play and read-only Lichess & Chess.com public endpoints.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Privacy & Permissions: Zero broad storage permissions. Device music uses Android Storage Access Framework (ACTION_OPEN_DOCUMENT). Games, puzzles, and preferences stay private locally unless signed in to a Gamebit server.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(stringResource(R.string.independent_notice), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { showLicensesDialog = true }) {
                            Text("Third-Party Notices & Licenses")
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showLicensesDialog) {
        GamebitDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = "Open Source Licenses",
            actions = { TextButton(onClick = { showLicensesDialog = false }) { Text("Close") } },
        ) {
            Text(
                "• AndroidX, Jetpack Compose, Room, DataStore, Navigation: Apache License 2.0\n" +
                    "• Retrofit, OkHttp (HTTPS + WebSocket) & Moshi: Apache License 2.0\n" +
                    "• Fonts (Space Grotesk, Plus Jakarta Sans, JetBrains Mono): SIL Open Font License 1.1\n" +
                    "• ChessHub Kotlin UCI Engine 2.0: Original pure-Kotlin Alpha-Beta / Quiescence / Multi-PV implementation.\n" +
                    "• External APIs: Official read-only Lichess & Chess.com public endpoints & Lichess Atom News feed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

data class ChessHubAchievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

fun computeChessHubAchievements(
    savedGames: List<LocalGameEntity>,
    myProfile: PublicProfileDoc?,
    settings: AppSettings,
): List<ChessHubAchievement> {
    val localGamesCount = savedGames.size
    val onlineGamesCount = (myProfile?.gamesPlayed ?: 0L).toInt()
    val totalGames = localGamesCount + onlineGamesCount

    val localWins = savedGames.count {
        (it.whiteName == "You" && it.result == "1-0") || (it.blackName == "You" && it.result == "0-1")
    }
    val onlineWins = (myProfile?.wins ?: 0L).toInt()
    val totalWins = localWins + onlineWins

    val checkmateWins = savedGames.count {
        ((it.whiteName == "You" && it.result == "1-0") || (it.blackName == "You" && it.result == "0-1")) &&
            it.resultReason.contains("checkmate", ignoreCase = true)
    }

    var currentWinStreak = 0
    var bestGameWinStreak = 0
    savedGames.sortedBy { it.createdAt }.forEach { game ->
        val isPlayerWin = (game.whiteName == "You" && game.result == "1-0") ||
            (game.blackName == "You" && game.result == "0-1")
        if (isPlayerWin) {
            currentWinStreak++
            if (currentWinStreak > bestGameWinStreak) bestGameWinStreak = currentWinStreak
        } else {
            currentWinStreak = 0
        }
    }

    val bestOnlineRating = listOfNotNull(
        myProfile?.bulletRating,
        myProfile?.blitzRating,
        myProfile?.rapidRating,
        myProfile?.classicalRating,
    ).maxOrNull()?.toInt() ?: 1500

    return listOf(
        ChessHubAchievement(
            id = "first_win",
            title = "First Win",
            description = "Win your first local or online game ($totalWins won)",
            unlocked = totalWins >= 1,
        ),
        ChessHubAchievement(
            id = "first_checkmate",
            title = "First Checkmate",
            description = "Deliver checkmate to win a game ($checkmateWins checkmates)",
            unlocked = checkmateWins >= 1,
        ),
        ChessHubAchievement(
            id = "ten_wins",
            title = "10 Wins",
            description = "Win 10 games across bots or online ($totalWins / 10)",
            unlocked = totalWins >= 10,
        ),
        ChessHubAchievement(
            id = "fifty_games",
            title = "50 Games",
            description = "Complete 50 rated or local games ($totalGames / 50)",
            unlocked = totalGames >= 50,
        ),
        ChessHubAchievement(
            id = "hundred_games",
            title = "100 Games",
            description = "Complete 100 games in ChessHub ($totalGames / 100)",
            unlocked = totalGames >= 100,
        ),
        ChessHubAchievement(
            id = "win_streak",
            title = "Win Streak",
            description = "Win 3 games in a row or reach Puzzle Streak 5 (Game Streak: $bestGameWinStreak)",
            unlocked = bestGameWinStreak >= 3 || settings.puzzleBestStreak >= 5,
        ),
        ChessHubAchievement(
            id = "rating_milestone",
            title = "Rating Milestone",
            description = "Reach 1600+ Online ELO or 1450+ Puzzle Rating (Puzzle: ${settings.localPuzzleRating})",
            unlocked = bestOnlineRating >= 1600 || settings.localPuzzleRating >= 1450,
        ),
        ChessHubAchievement(
            id = "speed_solver",
            title = "Speed Solver",
            description = "Score 10+ in Puzzle Storm/Racer (Best: ${settings.puzzleBestStorm})",
            unlocked = settings.puzzleBestStorm >= 10,
        ),
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
