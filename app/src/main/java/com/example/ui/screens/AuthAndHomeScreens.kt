package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.ChessVariant
import com.example.chess.CountryCatalog
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.TimeControl
import com.example.data.local.AppSettings
import com.example.data.local.LocalGameEntity
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.data.remote.PublicProfileDoc
import com.example.data.remote.TournamentDoc
import com.example.ui.components.drawChessPiece
import com.example.viewmodel.PlayMode

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun AuthScreen(
    isSigningIn: Boolean,
    defaultServerUrl: String,
    errorMessage: String?,
    onSignInClick: (serverUrl: String, usernameOrEmail: String, password: String) -> Unit,
    onRegisterClick: (serverUrl: String, username: String, email: String, password: String) -> Unit,
    onContinueOfflineClick: () -> Unit,
) {
    var authTab by remember { mutableStateOf(0) } // 0 = Sign In, 1 = Create Account
    var serverUrl by remember(defaultServerUrl) { mutableStateOf(defaultServerUrl) }
    var usernameOrEmail by remember { mutableStateOf("") }
    var registerUsername by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        drawChessPiece(Piece(PieceType.KING, PieceColor.WHITE), Offset.Zero, size.minDimension)
                    }
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "ChessHub Online & Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Connect to a ChessHub REST + WebSocket server for rated matchmaking, server-validated clocks, leaderboards, and clubs — or continue offline for complete local play, bots, analysis, puzzles, and lessons.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        TabRow(selectedTabIndex = authTab) {
                            Tab(selected = authTab == 0, onClick = { authTab = 0 }, text = { Text("Sign In") })
                            Tab(selected = authTab == 1, onClick = { authTab = 1 }, text = { Text("Create Account") })
                        }

                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("ChessHub Server URL (HTTPS)") },
                            leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_server_url_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )

                        if (authTab == 0) {
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Username or Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_identifier_input"),
                            )
                        } else {
                            OutlinedTextField(
                                value = registerUsername,
                                onValueChange = { registerUsername = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_username_input"),
                            )
                            OutlinedTextField(
                                value = registerEmail,
                                onValueChange = { registerEmail = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_email_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            )
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (authTab == 0) {
                                    onSignInClick(serverUrl, usernameOrEmail, password)
                                } else {
                                    onRegisterClick(serverUrl, registerUsername, registerEmail, password)
                                }
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("online_sign_in_button"),
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Connecting...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (authTab == 0) stringResource(R.string.sign_in_online) else "Register & Connect")
                            }
                        }

                        OutlinedButton(
                            onClick = onContinueOfflineClick,
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_offline_button"),
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.continue_offline))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.independent_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    isOnline: Boolean,
    settings: AppSettings,
    myProfile: PublicProfileDoc?,
    recentGames: List<LocalGameEntity>,
    activeLocalMoveCount: Int,
    tournaments: List<TournamentDoc>,
    isMusicPlaying: Boolean,
    onQuickPlayPreset: (TimeControl, ChessVariant, PlayMode) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onOpenPuzzleMode: () -> Unit,
    onOpenReviewForGame: (LocalGameEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onOpenNewsReader: () -> Unit,
    onToggleHomeCard: (String) -> Unit,
    onReorderHomeCard: (String, Int) -> Unit,
    onSwitchToOnlineSignIn: () -> Unit,
) {
    var showCustomizeSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { if (!isOnline) onSwitchToOnlineSignIn() },
                        ) {
                            Text(
                                text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    val playerSubtitle = if (myProfile != null) {
                        "${CountryCatalog.flagForIso(myProfile.countryIso)} ${myProfile.username} • ${myProfile.fideFederation} • Blitz ${myProfile.blitzRating}"
                    } else {
                        "${CountryCatalog.flagForIso(settings.countryIso)} ${settings.fideFederation} • Puzzle ${settings.localPuzzleRating}"
                    }
                    Text(
                        text = playerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row {
                    IconButton(
                        onClick = { showCustomizeSheet = true },
                        modifier = Modifier.testTag("customize_home_button"),
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Customize home cards")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button"),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            }
        }

        // Quick Play Presets Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Start",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            TimeControl.PRESETS[0],
                            TimeControl.PRESETS[3],
                            TimeControl.PRESETS[5],
                            TimeControl.PRESETS[6],
                        ).forEach { tc ->
                            FilledTonalButton(
                                onClick = { onQuickPlayPreset(tc, ChessVariant.STANDARD, PlayMode.VS_BOT) },
                                modifier = Modifier.testTag("quick_tc_${tc.label}"),
                            ) {
                                Text("${tc.label} • ${tc.category.replaceFirstChar { it.uppercase() }}")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onQuickPlayPreset(TimeControl.PRESETS[5], ChessVariant.STANDARD, PlayMode.VS_BOT) },
                            modifier = Modifier.weight(1f).testTag("home_play_bot_btn"),
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_bots))
                        }
                        OutlinedButton(
                            onClick = { onQuickPlayPreset(TimeControl.PRESETS[5], ChessVariant.STANDARD, PlayMode.PASS_AND_PLAY) },
                            modifier = Modifier.weight(1f).testTag("home_otb_btn"),
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("OTB")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenMusicPlayer,
                    modifier = Modifier.weight(1f).testTag("home_music_btn"),
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isMusicPlaying) "Music: Playing" else "Focus Music")
                }
                OutlinedButton(
                    onClick = onOpenNewsReader,
                    modifier = Modifier.weight(1f).testTag("home_news_btn"),
                ) {
                    Icon(Icons.Default.Newspaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chess News")
                }
            }
        }

        val visibleOrder = settings.homeCardOrder.filter { it !in settings.hiddenHomeCards }
        items(visibleOrder, key = { it }) { cardId ->
            when (cardId) {
                "active_game" -> HomeFeatureCard(
                    title = if (activeLocalMoveCount > 0) "Resume Active Game ($activeLocalMoveCount moves)" else "Start a New Board",
                    subtitle = "9 variants • Custom clocks • Premoves & Coach hints",
                    icon = Icons.Default.PlayArrow,
                    actionLabel = "Open Play",
                    testTag = "card_active_game",
                    onClick = { onNavigateToTab(1) },
                )
                "daily_puzzle" -> HomeFeatureCard(
                    title = stringResource(R.string.daily_puzzle),
                    subtitle = "Rated ${settings.localPuzzleRating} • Best Streak ${settings.puzzleBestStreak} • 16 tactical themes",
                    icon = Icons.Default.Extension,
                    actionLabel = "Solve Now",
                    testTag = "card_daily_puzzle",
                    onClick = onOpenPuzzleMode,
                )
                "recent_games" -> Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_recent_games"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Recent Games", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "Archive",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToTab(3) },
                            )
                        }
                        if (recentGames.isEmpty()) {
                            Text(
                                text = "No local games recorded yet. Finish a game or import PGN to review accuracy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            recentGames.take(3).forEach { g ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenReviewForGame(g) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${g.whiteName} vs ${g.blackName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${g.variant} • ${g.timeControl} • ${g.openingName.ifBlank { g.resultReason }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(
                                            text = g.result,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "recommended_lesson" -> {
                    val sampleLesson = PuzzlesAndLessonsCatalog.LESSONS.first()
                    HomeFeatureCard(
                        title = "Lesson: ${sampleLesson.title}",
                        subtitle = "${sampleLesson.level} • ${sampleLesson.summary}",
                        icon = Icons.Default.School,
                        actionLabel = "Study Lesson",
                        testTag = "card_recommended_lesson",
                        onClick = { onNavigateToTab(2) },
                    )
                }
                "upcoming_tournament" -> {
                    val nextTour = tournaments.firstOrNull()
                    HomeFeatureCard(
                        title = if (nextTour != null) "Tournament: ${nextTour.name}" else "Arena & Swiss Tournaments",
                        subtitle = if (nextTour != null) {
                            "${nextTour.format.uppercase()} • ${nextTour.timeControl} • ${nextTour.participantUids.size} players"
                        } else if (isOnline) {
                            "Create or join rated Arena, Swiss, or Simul events"
                        } else {
                            "Sign in to ChessHub Online to join live Arena & Swiss tournaments"
                        },
                        icon = Icons.Default.EmojiEvents,
                        actionLabel = if (isOnline) "Open Hub" else "Sign In",
                        testTag = "card_upcoming_tournament",
                        onClick = { if (isOnline) onNavigateToTab(4) else onSwitchToOnlineSignIn() },
                    )
                }
                "saved_analysis" -> HomeFeatureCard(
                    title = "Analysis Board & Game Review",
                    subtitle = "Multi-PV local engine • Eval graph • Opening Explorer • Board Editor",
                    icon = Icons.Default.Psychology,
                    actionLabel = "Analyze",
                    testTag = "card_saved_analysis",
                    onClick = { onNavigateToTab(3) },
                )
                "friend_activity" -> HomeFeatureCard(
                    title = "Clubs, Friends & Live TV",
                    subtitle = if (isOnline) "Challenge friends, chat, and watch live broadcasts" else "Sign in for Clubs, Direct Messages & Live Watch",
                    icon = Icons.Default.Group,
                    actionLabel = "Community",
                    testTag = "card_friend_activity",
                    onClick = { if (isOnline) onNavigateToTab(4) else onSwitchToOnlineSignIn() },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(onDismissRequest = { showCustomizeSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Customize Home Cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Show, hide, or reorder dashboard cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                settings.homeCardOrder.forEachIndexed { idx, id ->
                    val hidden = id in settings.hiddenHomeCards
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = homeCardDisplayTitle(id),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Row {
                            IconButton(
                                onClick = { onReorderHomeCard(id, -1) },
                                enabled = idx > 0,
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = { onReorderHomeCard(id, 1) },
                                enabled = idx < settings.homeCardOrder.lastIndex,
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(onClick = { onToggleHomeCard(id) }) {
                                Icon(
                                    imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (hidden) "Show card" else "Hide card",
                                    tint = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

private fun homeCardDisplayTitle(id: String): String = when (id) {
    "active_game" -> "Active Game"
    "daily_puzzle" -> "Daily Puzzle"
    "recent_games" -> "Recent Games"
    "recommended_lesson" -> "Recommended Lesson"
    "upcoming_tournament" -> "Upcoming Tournament"
    "saved_analysis" -> "Analysis & Review"
    "friend_activity" -> "Friends & Community"
    else -> id
}
