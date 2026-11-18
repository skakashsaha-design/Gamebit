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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.chess.CountryCatalog
import com.example.data.local.AppSettings
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.PublicProfileDoc
import com.example.ui.theme.ChessBoardThemes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAndLookupTab(
    isOnline: Boolean,
    settings: AppSettings,
    myProfile: PublicProfileDoc?,
    externalSummary: ExternalPlayerSummary?,
    onRequestSignIn: () -> Unit,
    onUpdateProfile: (String, String, String, String, Boolean) -> Unit,
    onLookupExternalPlayer: (String, String) -> Unit,
    onImportExternalPgn: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var username by remember(myProfile?.username) { mutableStateOf(myProfile?.username ?: "LocalPlayer") }
    var bio by remember(myProfile?.bio) { mutableStateOf(myProfile?.bio ?: "") }
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
                    Text(
                        text = "${CountryCatalog.flagForIso(countryIso)} Country: $countryIso  •  FIDE Federation: $fideFed",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (myProfile != null) {
                        Text(
                            text = "Bullet ${myProfile.bulletRating} • Blitz ${myProfile.blitzRating} • Rapid ${myProfile.rapidRating} • Classical ${myProfile.classicalRating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Games: ${myProfile.gamesPlayed} (${myProfile.wins}W / ${myProfile.draws}D / ${myProfile.losses}L)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text("Puzzle Rating (Local): ${settings.localPuzzleRating}", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCountryPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("Country: ${CountryCatalog.flagForIso(countryIso)} $countryIso")
                        }
                        OutlinedButton(onClick = { showFidePicker = true }, modifier = Modifier.weight(1f)) {
                            Text("FIDE: $fideFed")
                        }
                    }

                    if (isOnline) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                        )
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
                                onClick = { onUpdateProfile(username, bio, countryIso, fideFed, publicVisible) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Save Profile") }
                            OutlinedButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_btn")) {
                                Text(stringResource(R.string.sign_out))
                            }
                        }
                    } else {
                        Button(onClick = onRequestSignIn, modifier = Modifier.fillMaxWidth()) {
                            Text("Sign in with Google for Cloud Profile")
                        }
                    }
                }
            }
        }

        // Achievements
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Achievements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("• Tactician: Best Streak ${settings.puzzleBestStreak} (Unlock at 5)", style = MaterialTheme.typography.bodySmall)
                    Text("• Speed Solver: Best Storm ${settings.puzzleBestStorm} (Unlock at 10)", style = MaterialTheme.typography.bodySmall)
                    Text("• Opening Explorer: ${if ((myProfile?.gamesPlayed ?: 0) >= 1) "Unlocked" else "Play your first online game"}", style = MaterialTheme.typography.bodySmall)
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
    onShowCoordsChange: (Boolean) -> Unit,
    onShowLegalChange: (Boolean) -> Unit,
    onLowMemoryChange: (Boolean) -> Unit,
    onAutoQueenChange: (Boolean) -> Unit,
    onConfirmMoveChange: (Boolean) -> Unit,
    onPremoveChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onEvalBarChange: (Boolean) -> Unit,
    onThreatArrowChange: (Boolean) -> Unit,
    onAllowChatChange: (Boolean) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onLanguageChange: (String) -> Unit,
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
            item { Text("Appearance & Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
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

            item { Text("Gameplay & Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Show Coordinates", settings.showCoordinates, onShowCoordsChange) }
            item { SettingSwitchRow("Show Legal Moves", settings.showLegalMoves, onShowLegalChange) }
            item { SettingSwitchRow("Auto-Queen on Promotion", settings.autoQueen, onAutoQueenChange) }
            item { SettingSwitchRow("Confirm Moves Before Sending", settings.confirmMoves, onConfirmMoveChange) }
            item { SettingSwitchRow("Enable Premoves", settings.premoveEnabled, onPremoveChange) }
            item { SettingSwitchRow("Show Evaluation Bar", settings.showEvalBar, onEvalBarChange) }
            item { SettingSwitchRow("Show Opponent Threat Arrow", settings.showThreatArrow, onThreatArrowChange) }
            item { SettingSwitchRow("Allow Online Game Chat", settings.allowChatInGames, onAllowChatChange) }

            item { Text("Audio, Haptics & Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SettingSwitchRow("Board Sounds", settings.soundEnabled, onSoundChange) }
            item {
                Text("Sound Volume: ${(settings.soundVolume * 100).toInt()}%")
                Slider(value = settings.soundVolume, onValueChange = onSoundVolumeChange, valueRange = 0f..1f)
            }
            item { SettingSwitchRow("Haptic Feedback", settings.hapticsEnabled, onHapticsChange) }
            item {
                SettingSwitchRow(
                    title = if (isLowRamDevice) "Low-Memory Mode (Recommended)" else "Low-Memory Mode (Android Go/1GB)",
                    checked = settings.lowMemoryMode,
                    onCheckedChange = onLowMemoryChange,
                )
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
                        "• ChessHub local engine: Pure Kotlin Alpha-Beta / Quiescence / Multi-PV search.\n" +
                        "• External APIs: Official read-only Lichess & Chess.com public endpoints.",
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
