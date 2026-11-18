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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.chess.CountryCatalog
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.local.AppSettings
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.PublicProfileDoc
import com.example.ui.components.PieceMiniIcon
import com.example.ui.theme.ChessBoardThemes
import com.example.ui.theme.ChessPieceStyles
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAndLookupTab(
    isOnline: Boolean,
    settings: AppSettings,
    myProfile: PublicProfileDoc?,
    externalSummary: ExternalPlayerSummary?,
    onRequestSignIn: () -> Unit,
    onUpdateProfile: (String, String, String, String, Boolean) -> Unit,
    onUpdateLocalIdentity: (String, String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onLookupExternalPlayer: (String, String) -> Unit,
    onImportExternalPgn: (String) -> Unit,
    onSignOut: () -> Unit,
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

    var extPlatform by remember { mutableStateOf("Lichess") }
    var extUsername by remember { mutableStateOf("") }

    val avatarOptions = listOf(
        "king" to PieceType.KING,
        "queen" to PieceType.QUEEN,
        "rook" to PieceType.ROOK,
        "bishop" to PieceType.BISHOP,
        "knight" to PieceType.KNIGHT,
        "pawn" to PieceType.PAWN,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Player Profile & Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose Piece Avatar", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        avatarOptions.forEach { (id, type) ->
                            val selected = settings.localAvatarId == id
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { onAvatarSelected(id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PieceMiniIcon(
                                        piece = Piece(type, PieceColor.WHITE),
                                        modifier = Modifier.size(34.dp),
                                        pieceStyleId = settings.pieceStyleId,
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
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onUpdateLocalIdentity(username, bio) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Save Local Profile") }
                            Button(onClick = onRequestSignIn, modifier = Modifier.weight(1f)) {
                                Text("Google Sign-In")
                            }
                        }
                    }
                }
            }
        }

        // Ratings & Win/Draw/Loss Breakdown
        item {
            val gamesPlayed = (myProfile?.gamesPlayed ?: 0L).toInt()
            val wins = (myProfile?.wins ?: 0L).toInt()
            val draws = (myProfile?.draws ?: 0L).toInt()
            val losses = (myProfile?.losses ?: 0L).toInt()
            val winPct = if (gamesPlayed > 0) ((wins * 100f) / gamesPlayed).roundToInt() else 0
            val drawPct = if (gamesPlayed > 0) ((draws * 100f) / gamesPlayed).roundToInt() else 0
            val lossPct = if (gamesPlayed > 0) (100 - winPct - drawPct).coerceAtLeast(0) else 0

            val bulletR = (myProfile?.bulletRating ?: 1500L).toInt()
            val blitzR = (myProfile?.blitzRating ?: 1500L).toInt()
            val rapidR = (myProfile?.rapidRating ?: 1500L).toInt()
            val classicalR = (myProfile?.classicalRating ?: 1500L).toInt()
            val puzzleR = settings.localPuzzleRating

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ratings & Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Bullet $bulletR • Blitz $blitzR • Rapid $rapidR • Classical $classicalR • Puzzle $puzzleR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        val bars = listOf(bulletR, blitzR, rapidR, classicalR, puzzleR)
                        val maxR = (bars.maxOrNull() ?: 2000).coerceAtLeast(2000).toFloat()
                        val w = size.width / (bars.size * 1.6f)
                        val gap = (size.width - w * bars.size) / (bars.size + 1)
                        bars.forEachIndexed { idx, r ->
                            val frac = (r / maxR).coerceIn(0.15f, 1f)
                            val bh = size.height * frac
                            drawRect(
                                color = if (idx == 4) Color(0xFFF59E0B) else Color(0xFF10B981),
                                topLeft = Offset(gap + idx * (w + gap), size.height - bh),
                                size = Size(w, bh),
                            )
                        }
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

        // Badges & Titles
        item {
            val badges = listOf(
                Triple("Club Player", "Active ChessHub profile & local archive", true),
                Triple("Tactician", "Reach a Puzzle Streak of 5+ (Best: ${settings.puzzleBestStreak})", settings.puzzleBestStreak >= 5),
                Triple("Speed Solver", "Score 10+ in Puzzle Storm/Racer (Best: ${settings.puzzleBestStorm})", settings.puzzleBestStorm >= 10),
                Triple("Tactical Master", "Reach 1450+ Puzzle Rating (Current: ${settings.localPuzzleRating})", settings.localPuzzleRating >= 1450),
                Triple("Contender", "Complete an online rated ChessHub match", (myProfile?.gamesPlayed ?: 0L) >= 1L),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Titles, Badges & Awards", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    badges.forEach { (title, desc, unlocked) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (unlocked) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Country") },
        text = {
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
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select FIDE Federation") },
        text = {
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
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
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
                Text("Animation Speed: ${String.format("%.1fx", settings.animationSpeed)}")
                Slider(value = settings.animationSpeed, onValueChange = onAnimationSpeedChange, valueRange = 0.5f..2.0f)
            }
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
                        Triple("move", "Move", settings.sfxMoveEnabled),
                        Triple("capture", "Capture", settings.sfxCaptureEnabled),
                        Triple("check", "Check", settings.sfxCheckEnabled),
                        Triple("checkmate", "Mate/End", settings.sfxCheckmateEnabled),
                        Triple("promotion", "Promotion", settings.sfxPromotionEnabled),
                        Triple("feedback", "Puzzle SFX", settings.sfxFeedbackEnabled),
                        Triple("button", "UI Click", settings.sfxButtonEnabled),
                        Triple("book", "Book Move", settings.sfxBookEnabled),
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
            item { SettingSwitchRow("Bot Personality Voice (TTS)", settings.botVoiceEnabled, onBotVoiceChange) }
            item { SettingSwitchRow("Coach / Trainer Hint Voice (TTS)", settings.trainerVoiceEnabled, onTrainerVoiceChange) }
            item {
                Text("Voice Volume: ${(settings.voiceVolume * 100).toInt()}%")
                Slider(value = settings.voiceVolume, onValueChange = onVoiceVolumeChange, valueRange = 0f..1f)
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
                        Text("About ChessHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                        Text("Package: ${BuildConfig.APPLICATION_ID}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "ChessHub combines pure-Kotlin offline chess intelligence (ChessHub Kotlin UCI Engine 2.0, 9 variants, 18 bots, Mistake Breakdown, Puzzles & Opening Drills) with Firebase real-time rated online play and read-only Lichess & Chess.com public endpoints.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Privacy & Permissions: Zero broad storage permissions. Device music uses Android Storage Access Framework (ACTION_OPEN_DOCUMENT). Games, puzzles, and preferences stay private locally unless signed in.",
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
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Text(
                    "• AndroidX, Jetpack Compose, Room, DataStore, Navigation, Credentials: Apache License 2.0\n" +
                        "• Firebase Android SDKs: Apache License 2.0\n" +
                        "• OkHttp & Moshi: Apache License 2.0\n" +
                        "• Fonts (Space Grotesk, Plus Jakarta Sans, JetBrains Mono): SIL Open Font License 1.1\n" +
                        "• ChessHub Kotlin UCI Engine 2.0: Original pure-Kotlin Alpha-Beta / Quiescence / Multi-PV implementation.\n" +
                        "• External APIs: Official read-only Lichess & Chess.com public endpoints & Lichess Atom News feed.",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = { TextButton(onClick = { showLicensesDialog = false }) { Text("Close") } },
        )
    }
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
