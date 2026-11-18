package com.example

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.MusicPlayContext
import com.example.audio.MusicPlayerManager
import com.example.audio.SoundAndHapticsManager
import com.example.audio.VoiceCoachManager
import com.example.auth.GoogleAuthHelper
import com.example.chess.ChessVariant
import com.example.data.local.AppSettings
import com.example.data.local.ChessHubDatabase
import com.example.data.local.CollectionEntity
import com.example.data.local.LocalChessRepository
import com.example.data.local.SettingsRepository
import com.example.data.remote.ChessHubOnlineRepository
import com.example.data.remote.ExternalBroadcastItem
import com.example.data.remote.ExternalChessApis
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.ExternalTvChannel
import com.example.ui.screens.AnalyzeScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChessNewsScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.MusicPlayerScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.SettingsAndAboutScreen
import com.example.ui.theme.ChessHubTheme
import com.example.viewmodel.AnalysisAndReviewViewModel
import com.example.viewmodel.LocalGameViewModel
import com.example.viewmodel.OnlineViewModel
import com.example.viewmodel.PlayMode
import com.example.viewmodel.PuzzleMode
import com.example.viewmodel.TrainingViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

sealed interface AppAuthMode {
    data object Pending : AppAuthMode
    data object Unauthenticated : AppAuthMode
    data object OfflineGuest : AppAuthMode
    data class Authenticated(val user: FirebaseUser) : AppAuthMode
}

class MainActivity : ComponentActivity() {
    private var initialSharedPayload by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initialSharedPayload = extractIncomingChessPayload(intent)
        setContent {
            ChessHubRoot(
                incomingPayload = initialSharedPayload,
                onConsumeIncomingPayload = { initialSharedPayload = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingChessPayload(intent)?.let { initialSharedPayload = it }
    }

    private fun extractIncomingChessPayload(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        }
        val uri = intent.data ?: return null
        if (uri.scheme == "chesshub") {
            return uri.getQueryParameter("fen")
                ?: uri.getQueryParameter("pgn")
                ?: uri.path?.trimStart('/')
        }
        return null
    }
}

private fun FirebaseAuth.authUserFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun ChessHubRoot(
    incomingPayload: String? = null,
    onConsumeIncomingPayload: () -> Unit = {},
) {
    val baseContext = LocalContext.current
    val appContext = baseContext.applicationContext

    val settingsRepo = remember { SettingsRepository(appContext) }
    val db = remember { ChessHubDatabase.getInstance(appContext) }
    val localRepo = remember {
        LocalChessRepository(
            dao = db.chessHubDao(),
            puzzleDao = db.chessPuzzleDao(),
            mediaDao = db.mediaAndNewsDao(),
        )
    }
    val soundManager = remember { SoundAndHapticsManager(appContext) }
    val voiceCoach = remember { VoiceCoachManager(appContext) }
    val musicManager = remember { MusicPlayerManager(appContext) }
    val externalApis = remember { ExternalChessApis() }

    DisposableEffect(Unit) {
        onDispose {
            voiceCoach.shutdown()
            musicManager.release()
        }
    }

    val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val baseConfig = androidx.compose.ui.platform.LocalConfiguration.current
    val localizedConfig = remember(baseConfig, settings.languageCode) {
        Configuration(baseConfig).apply {
            val locale = Locale.forLanguageTag(settings.languageCode)
            setLocale(locale)
        }
    }
    @Suppress("DEPRECATION")
    remember(settings.languageCode) {
        baseContext.resources.updateConfiguration(localizedConfig, baseContext.resources.displayMetrics)
        true
    }

    val firebaseReady = remember(appContext) {
        if (com.google.firebase.FirebaseApp.getApps(appContext).isNotEmpty()) true
        else runCatching { com.google.firebase.FirebaseApp.initializeApp(appContext) != null }.getOrDefault(false)
    }

    val credentialManager = remember(baseContext) { CredentialManager.create(baseContext) }
    val currentUser by remember(firebaseReady) {
        if (firebaseReady) Firebase.auth.authUserFlow() else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = if (firebaseReady) Firebase.auth.currentUser else null)

    var autoSignInDone by remember { mutableStateOf(!firebaseReady) }
    var forceOffline by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val rootScope = rememberCoroutineScope()

    LaunchedEffect(firebaseReady) {
        if (!firebaseReady) {
            autoSignInDone = true
            return@LaunchedEffect
        }
        if (Firebase.auth.currentUser != null) {
            autoSignInDone = true
        } else {
            GoogleAuthHelper.attemptAutoSignIn(
                context = baseContext,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    autoSignInDone = true
                    forceOffline = false
                },
                onUnauthenticated = { autoSignInDone = true },
                scope = rootScope,
            )
        }
    }

    val authMode = when {
        currentUser != null -> AppAuthMode.Authenticated(currentUser!!)
        forceOffline -> AppAuthMode.OfflineGuest
        !autoSignInDone -> AppAuthMode.Pending
        else -> AppAuthMode.Unauthenticated
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides localizedConfig) {
        ChessHubTheme(
            darkTheme = settings.darkTheme,
            highContrast = settings.highContrast,
            dynamicColor = settings.dynamicColor,
            fontScale = settings.fontScale,
        ) {
            when (authMode) {
                AppAuthMode.Pending -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                AppAuthMode.Unauthenticated -> {
                    AuthScreen(
                        isSigningIn = isSigningIn,
                        errorMessage = authError,
                        onGoogleSignInClick = {
                            isSigningIn = true
                            authError = null
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = baseContext,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    forceOffline = false
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    authError = msg
                                },
                                scope = rootScope,
                                onAuthCancelled = { isSigningIn = false },
                            )
                        },
                        onContinueOfflineClick = { forceOffline = true },
                    )
                }
                AppAuthMode.OfflineGuest -> {
                    ChessHubMainShell(
                        isOnline = false,
                        onlineUser = null,
                        settings = settings,
                        settingsRepo = settingsRepo,
                        localRepo = localRepo,
                        soundManager = soundManager,
                        voiceCoach = voiceCoach,
                        musicManager = musicManager,
                        externalApis = externalApis,
                        incomingPayload = incomingPayload,
                        onConsumeIncomingPayload = onConsumeIncomingPayload,
                        onSwitchToOnlineSignIn = { forceOffline = false },
                        onSignOut = { forceOffline = false },
                    )
                }
                is AppAuthMode.Authenticated -> {
                    key(authMode.user.uid) {
                        ChessHubMainShell(
                            isOnline = true,
                            onlineUser = authMode.user,
                            settings = settings,
                            settingsRepo = settingsRepo,
                            localRepo = localRepo,
                            soundManager = soundManager,
                            voiceCoach = voiceCoach,
                            musicManager = musicManager,
                            externalApis = externalApis,
                            incomingPayload = incomingPayload,
                            onConsumeIncomingPayload = onConsumeIncomingPayload,
                            onSwitchToOnlineSignIn = {},
                            onSignOut = {
                                GoogleAuthHelper.signOut(credentialManager, rootScope) {
                                    forceOffline = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessHubMainShell(
    isOnline: Boolean,
    onlineUser: FirebaseUser?,
    settings: AppSettings,
    settingsRepo: SettingsRepository,
    localRepo: LocalChessRepository,
    soundManager: SoundAndHapticsManager,
    voiceCoach: VoiceCoachManager,
    musicManager: MusicPlayerManager,
    externalApis: ExternalChessApis,
    incomingPayload: String?,
    onConsumeIncomingPayload: () -> Unit,
    onSwitchToOnlineSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val localGameVm: LocalGameViewModel = viewModel(
        factory = simpleVmFactory { LocalGameViewModel(localRepo, soundManager, voiceCoach) },
    )
    val analysisVm: AnalysisAndReviewViewModel = viewModel(
        factory = simpleVmFactory { AnalysisAndReviewViewModel(localRepo, externalApis) },
    )
    val trainingVm: TrainingViewModel = viewModel(
        factory = simpleVmFactory { TrainingViewModel(localRepo, settingsRepo, externalApis, voiceCoach) },
    )

    // Strictly instantiate OnlineViewModel ONLY when authenticated
    val onlineVm: OnlineViewModel? = if (isOnline && onlineUser != null) {
        viewModel(
            key = "online_${onlineUser.uid}",
            factory = simpleVmFactory {
                OnlineViewModel(
                    repo = ChessHubOnlineRepository(context.applicationContext),
                    externalApis = externalApis,
                    currentUserId = onlineUser.uid,
                    initialDisplayName = onlineUser.displayName ?: "Player",
                    countryIso = settings.countryIso,
                    fideFederation = settings.fideFederation,
                )
            },
        )
    } else null

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showMusicPlayer by remember { mutableStateOf(false) }
    var showNewsReader by remember { mutableStateOf(false) }

    val userMusicTracks by localRepo.observeMusicTracks().collectAsStateWithLifecycle(initialValue = emptyList())
    val musicState by musicManager.uiState.collectAsStateWithLifecycle()
    val cachedNews by localRepo.observeCachedNews().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(userMusicTracks) {
        musicManager.syncUserTracks(userMusicTracks)
    }

    LaunchedEffect(settings) {
        localGameVm.updateSettings(settings)
        analysisVm.updateSettings(settings)
        trainingVm.updateSettings(settings)
    }

    val musicContext = when (selectedTab) {
        1 -> MusicPlayContext.GAME
        2 -> MusicPlayContext.TRAINING
        else -> MusicPlayContext.MENU
    }
    LaunchedEffect(settings, musicContext) {
        musicManager.applySettings(settings, musicContext)
    }

    LaunchedEffect(incomingPayload) {
        val payload = incomingPayload ?: return@LaunchedEffect
        val loaded = if (payload.contains("[Event ") || payload.contains("1.")) {
            analysisVm.importPgn(payload)
        } else {
            analysisVm.loadFen(payload)
        }
        if (loaded) selectedTab = 3
        onConsumeIncomingPayload()
    }

    val localGameState by localGameVm.uiState.collectAsStateWithLifecycle()
    val analysisState by analysisVm.uiState.collectAsStateWithLifecycle()
    val puzzleState by trainingVm.puzzleState.collectAsStateWithLifecycle()
    val puzzleCollection by trainingVm.puzzleCollection.collectAsStateWithLifecycle()
    val puzzleAttempts by trainingVm.puzzleAttempts.collectAsStateWithLifecycle()
    val lessonProgress by trainingVm.lessonProgress.collectAsStateWithLifecycle()
    val drillState by trainingVm.drillState.collectAsStateWithLifecycle()
    val coordState by trainingVm.coordState.collectAsStateWithLifecycle()

    val savedGames by localRepo.observeGames().collectAsStateWithLifecycle(initialValue = emptyList())
    val studies by localRepo.observeStudies().collectAsStateWithLifecycle(initialValue = emptyList())
    val collections by localRepo.observeCollections().collectAsStateWithLifecycle(initialValue = emptyList())

    // Offline public external watch fallback state (zero Firestore)
    var offlineBroadcasts by remember { mutableStateOf<List<ExternalBroadcastItem>>(emptyList()) }
    var offlineTv by remember { mutableStateOf<List<ExternalTvChannel>>(emptyList()) }
    var offlineExtPlayer by remember { mutableStateOf<ExternalPlayerSummary?>(null) }

    // Online state subscriptions (when onlineVm != null)
    val onlineStatusMsg by (onlineVm?.statusMessage ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val onlineIsAdmin by (onlineVm?.isAdmin ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val myProfileState by (onlineVm?.myProfile ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val myProfile = (myProfileState as? UiState.Success)?.data
    val searchingTicketId by (onlineVm?.searchingTicketId ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val activeOnlineGame by (onlineVm?.activeGameDoc ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val onlineChat by (onlineVm?.activeGameChat ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val onlineRatingDelta by (onlineVm?.lastRatingDelta ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val playerSearchResults by (onlineVm?.playerSearchResults ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val tournamentsState by (onlineVm?.tournaments ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val tournamentEntries by (onlineVm?.selectedTournamentEntries ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val clubsState by (onlineVm?.clubs ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val teamsState by (onlineVm?.teams ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val liveWatchState by (onlineVm?.liveWatchGames ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val leaderboardState by (onlineVm?.leaderboard ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val friendshipsState by (onlineVm?.friendships ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val threadsState by (onlineVm?.messageThreads ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val directMessages by (onlineVm?.directMessages ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val publicStudiesState by (onlineVm?.publicStudies ?: MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val onlineBroadcasts by (onlineVm?.broadcasts ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val onlineTv by (onlineVm?.tvChannels ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val onlineExtPlayer by (onlineVm?.externalPlayerSummary ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val adminReports by (onlineVm?.adminReports ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()

    LaunchedEffect(onlineStatusMsg) {
        val msg = onlineStatusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onlineVm?.clearStatusMessage()
    }

    LaunchedEffect(activeOnlineGame?.id) {
        if (activeOnlineGame != null) selectedTab = 1
    }

    BackHandler(enabled = showSettings || showMusicPlayer || showNewsReader || selectedTab != 0) {
        when {
            showMusicPlayer -> showMusicPlayer = false
            showNewsReader -> showNewsReader = false
            showSettings -> showSettings = false
            else -> selectedTab = 0
        }
    }

    if (showMusicPlayer) {
        MusicPlayerScreen(
            musicState = musicState,
            musicManager = musicManager,
            settings = settings,
            onAddPickedTracks = { picked ->
                scope.launch {
                    picked.forEach { track -> localRepo.saveMusicTrack(track) }
                }
            },
            onDeleteTrack = { id ->
                scope.launch { localRepo.deleteMusicTrack(id) }
            },
            onMusicEnabledChange = { scope.launch { settingsRepo.setMusicEnabled(it) } },
            onMusicVolumeChange = { scope.launch { settingsRepo.setMusicVolume(it) } },
            onMusicScopeChange = { sc, en -> scope.launch { settingsRepo.setMusicScope(sc, en) } },
            onLowMemAudioChange = { scope.launch { settingsRepo.setLowMemoryAudioMode(it) } },
            onBack = { showMusicPlayer = false },
        )
        return
    }

    if (showNewsReader) {
        ChessNewsScreen(
            cachedNews = cachedNews,
            externalApis = externalApis,
            onSaveCachedNews = { items -> scope.launch { localRepo.saveCachedNews(items) } },
            onClearNewsCache = { scope.launch { localRepo.clearNewsCache() } },
            onBack = { showNewsReader = false },
        )
        return
    }

    if (showSettings) {
        SettingsAndAboutScreen(
            settings = settings,
            isLowRamDevice = settingsRepo.isDeviceLowRam,
            onBack = { showSettings = false },
            onDarkThemeChange = { scope.launch { settingsRepo.setDarkTheme(it) } },
            onHighContrastChange = { scope.launch { settingsRepo.setHighContrast(it) } },
            onBoardThemeChange = { scope.launch { settingsRepo.setBoardTheme(it) } },
            onPieceStyleChange = { scope.launch { settingsRepo.setPieceStyle(it) } },
            onShowCoordsChange = { scope.launch { settingsRepo.setShowCoordinates(it) } },
            onShowLegalChange = { scope.launch { settingsRepo.setShowLegalMoves(it) } },
            onShowLastMoveChange = { scope.launch { settingsRepo.setShowLastMove(it) } },
            onMoveAnimationChange = { scope.launch { settingsRepo.setMoveAnimation(it) } },
            onReducedAnimationsChange = { scope.launch { settingsRepo.setReducedAnimations(it) } },
            onAnimationSpeedChange = { scope.launch { settingsRepo.setAnimationSpeed(it) } },
            onLowMemoryChange = { scope.launch { settingsRepo.setLowMemoryMode(it) } },
            onAutoQueenChange = { scope.launch { settingsRepo.setAutoQueen(it) } },
            onConfirmMoveChange = { scope.launch { settingsRepo.setConfirmMoves(it) } },
            onPremoveChange = { scope.launch { settingsRepo.setPremoveEnabled(it) } },
            onCoordNotationChange = { scope.launch { settingsRepo.setCoordinateNotation(it) } },
            onSoundChange = { scope.launch { settingsRepo.setSoundEnabled(it) } },
            onSoundVolumeChange = { scope.launch { settingsRepo.setSoundVolume(it) } },
            onSfxFlagChange = { cat, en -> scope.launch { settingsRepo.setSfxFlag(cat, en) } },
            onHapticsChange = { scope.launch { settingsRepo.setHapticsEnabled(it) } },
            onBotVoiceChange = { scope.launch { settingsRepo.setBotVoiceEnabled(it) } },
            onTrainerVoiceChange = { scope.launch { settingsRepo.setTrainerVoiceEnabled(it) } },
            onVoiceVolumeChange = { scope.launch { settingsRepo.setVoiceVolume(it) } },
            onMusicEnabledChange = { scope.launch { settingsRepo.setMusicEnabled(it) } },
            onMusicVolumeChange = { scope.launch { settingsRepo.setMusicVolume(it) } },
            onMusicScopeChange = { sc, en -> scope.launch { settingsRepo.setMusicScope(sc, en) } },
            onLowMemAudioChange = { scope.launch { settingsRepo.setLowMemoryAudioMode(it) } },
            onOpenMusicPlayer = { showMusicPlayer = true },
            onEvalBarChange = { scope.launch { settingsRepo.setShowEvalBar(it) } },
            onBestMoveArrowChange = { scope.launch { settingsRepo.setShowBestMoveArrow(it) } },
            onThreatArrowChange = { scope.launch { settingsRepo.setShowThreatArrow(it) } },
            onAllowBoardArrowsChange = { scope.launch { settingsRepo.setAllowBoardArrows(it) } },
            onEngineDepthChange = { scope.launch { settingsRepo.setEngineDepth(it) } },
            onEngineMultiPvChange = { scope.launch { settingsRepo.setEngineMultiPv(it) } },
            onOpeningBookChange = { scope.launch { settingsRepo.setOpeningRecognitionFlag("book", it) } },
            onOpeningRecogChange = { scope.launch { settingsRepo.setOpeningRecognitionFlag("opening", it) } },
            onGambitRecogChange = { scope.launch { settingsRepo.setOpeningRecognitionFlag("gambit", it) } },
            onEndgameRecogChange = { scope.launch { settingsRepo.setOpeningRecognitionFlag("endgame", it) } },
            onAllowChatChange = { scope.launch { settingsRepo.setAllowChatInGames(it) } },
            onSocialFeedChange = { scope.launch { settingsRepo.setShowSocialFeed(it) } },
            onNotificationsChange = { scope.launch { settingsRepo.setNotificationsEnabled(it) } },
            onFontScaleChange = { scope.launch { settingsRepo.setFontScale(it) } },
            onLanguageChange = { scope.launch { settingsRepo.setLanguage(it) } },
            onClearNewsCache = {
                scope.launch {
                    localRepo.clearNewsCache()
                    snackbarHostState.showSnackbar("Cleared cached chess news articles")
                }
            },
        )
        return
    }

    val navItems = listOf(
        Triple(stringResource(R.string.nav_home), Icons.Default.Home, "nav_home"),
        Triple(stringResource(R.string.nav_play), Icons.Default.PlayArrow, "nav_play"),
        Triple(stringResource(R.string.nav_learn), Icons.Default.School, "nav_learn"),
        Triple(stringResource(R.string.nav_analyze), Icons.Default.Assessment, "nav_analyze"),
        Triple(stringResource(R.string.nav_community), Icons.Default.Groups, "nav_hub"),
    )

    fun shareText(title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, (label, icon, tag) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEachIndexed { idx, (label, icon, tag) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            isOnline = isOnline,
                            settings = settings,
                            myProfile = myProfile,
                            recentGames = savedGames,
                            activeLocalMoveCount = localGameState.moveHistorySan.size,
                            tournaments = (tournamentsState as? UiState.Success)?.data ?: emptyList(),
                            isMusicPlaying = musicState.isPlaying,
                            onQuickPlayPreset = { tc, variant, mode ->
                                localGameVm.startNewGame(mode, variant, tc, localGameState.humanColor)
                                selectedTab = 1
                            },
                            onNavigateToTab = { selectedTab = it },
                            onOpenPuzzleMode = {
                                trainingVm.selectPuzzleMode(PuzzleMode.DAILY)
                                selectedTab = 2
                            },
                            onOpenReviewForGame = { game ->
                                analysisVm.loadSavedGame(game)
                                selectedTab = 3
                            },
                            onOpenSettings = { showSettings = true },
                            onOpenMusicPlayer = { showMusicPlayer = true },
                            onOpenNewsReader = { showNewsReader = true },
                            onToggleHomeCard = { cardId ->
                                scope.launch { settingsRepo.toggleHomeCard(cardId, settings.hiddenHomeCards) }
                            },
                            onReorderHomeCard = { cardId, delta ->
                                val order = settings.homeCardOrder.toMutableList()
                                val idx = order.indexOf(cardId)
                                val target = idx + delta
                                if (idx >= 0 && target in order.indices) {
                                    order.removeAt(idx)
                                    order.add(target, cardId)
                                    scope.launch { settingsRepo.setHomeCardOrder(order) }
                                }
                            },
                            onSwitchToOnlineSignIn = onSwitchToOnlineSignIn,
                        )
                        1 -> PlayScreen(
                            localState = localGameState,
                            settings = settings,
                            isOnline = isOnline,
                            currentUserId = onlineUser?.uid,
                            myProfile = myProfile,
                            searchingTicketId = searchingTicketId,
                            activeOnlineGame = activeOnlineGame,
                            onlineChatMessages = onlineChat,
                            onlineRatingDelta = onlineRatingDelta,
                            onLocalMove = { localGameVm.onUserMove(it) },
                            onQueuePremove = { localGameVm.queuePremove(it) },
                            onClearPremoves = { localGameVm.clearPremoves() },
                            onConfirmMove = { localGameVm.confirmPendingMove() },
                            onCancelConfirmMove = { localGameVm.cancelPendingMove() },
                            onSelectDropPiece = { localGameVm.selectDropPiece(it) },
                            onStartNewLocalGame = { mode, variant, tc, color, bot, botBlack ->
                                localGameVm.startNewGame(mode, variant, tc, color, bot, botBlack)
                            },
                            onToggleBotVsBotPause = { localGameVm.toggleBotVsBotPause() },
                            onStepBotVsBotNext = { localGameVm.stepBotVsBotNext() },
                            onToggleFlip = { localGameVm.toggleFlip() },
                            onToggleClockPause = { localGameVm.toggleClockPause() },
                            onTakeback = { localGameVm.takebackMove() },
                            onRequestHint = { localGameVm.requestHint() },
                            onResignLocal = { localGameVm.resign() },
                            onAgreeDrawLocal = { localGameVm.agreeDraw() },
                            onAnalyzePgn = { pgn ->
                                if (analysisVm.importPgn(pgn)) selectedTab = 3
                            },
                            onShareText = ::shareText,
                            onStartMatchmaking = { v, tc, rated -> onlineVm?.startQuickMatchmaking(v, tc, rated) },
                            onCancelMatchmaking = { onlineVm?.cancelMatchmaking() },
                            onOnlineMove = { m -> onlineVm?.playOnlineMove(m) },
                            onResignOnline = { onlineVm?.resignOnlineGame() },
                            onOfferOnlineDraw = { onlineVm?.offerOnlineDraw() },
                            onSendOnlineChat = { txt -> onlineVm?.sendGameChat(txt) },
                            onCloseOnlineGame = {
                                onlineVm?.clearLastRatingDelta()
                                onlineVm?.closeActiveOnlineGame()
                            },
                            onRequestOnlineSignIn = onSwitchToOnlineSignIn,
                            onJumpToPly = { localGameVm.jumpToPly(it) },
                            onStepFirstMove = { localGameVm.stepFirstMove() },
                            onStepPreviousMove = { localGameVm.stepPreviousMove() },
                            onStepNextMove = { localGameVm.stepNextMove() },
                            onStepLatestMove = { localGameVm.stepLatestMove() },
                            onExportLocalPgn = { localGameVm.exportCurrentPgn() },
                            onImportLocalPgn = { pgn -> localGameVm.importPgnIntoLocalGame(pgn) },
                        )
                        2 -> LearnScreen(
                            settings = settings,
                            puzzleState = puzzleState,
                            puzzleCollection = puzzleCollection,
                            puzzleAttempts = puzzleAttempts,
                            lessonProgress = lessonProgress,
                            drillState = drillState,
                            coordState = coordState,
                            onSelectPuzzleMode = { m, th -> trainingVm.selectPuzzleMode(m, th) },
                            onSelectTheme = { th -> trainingVm.selectThemeFilter(th) },
                            onSelectDifficultyBand = { band -> trainingVm.selectDifficultyBand(band) },
                            onSubmitPuzzleMove = { m -> trainingVm.submitPuzzleMove(m) },
                            onRetryPuzzle = { trainingVm.retryPuzzle() },
                            onNextPuzzle = { trainingVm.nextPuzzle() },
                            onRevealHint = { trainingVm.revealHint() },
                            onToggleBookmarkPuzzle = { trainingVm.toggleBookmarkCurrentPuzzle() },
                            onCreateCustomPuzzle = { title, fen, sol, rating, theme, hint, exp ->
                                trainingVm.createCustomPuzzle(title, fen, sol, rating, theme, hint, exp)
                            },
                            onDeleteCustomPuzzle = { id -> trainingVm.deletePuzzle(id) },
                            onLoadSpecificPuzzle = { pz -> trainingVm.loadSpecificPuzzle(pz) },
                            onCompleteLessonStep = { lesson, step -> trainingVm.markLessonStepCompleted(lesson, step) },
                            onStartDrill = { drill -> trainingVm.startOpeningDrill(drill) },
                            onSubmitDrillMove = { m -> trainingVm.submitDrillMove(m) },
                            onStartCoordTrainer = { fl -> trainingVm.startCoordinateTrainer(fl) },
                            onTapCoordSquare = { sq -> trainingVm.onCoordinateSquareTapped(sq) },
                            onPracticeEndgameInPlay = { fen ->
                                localGameVm.startNewGame(
                                    mode = PlayMode.VS_BOT,
                                    variant = ChessVariant.STANDARD,
                                    timeControl = localGameState.timeControl,
                                    humanColor = com.example.chess.PieceColor.WHITE,
                                    customInitialFen = fen,
                                )
                                selectedTab = 1
                            },
                        )
                        3 -> AnalyzeScreen(
                            state = analysisState,
                            settings = settings,
                            isOnline = isOnline,
                            savedGames = savedGames,
                            studies = studies,
                            collections = collections,
                            onPlayMove = { analysisVm.playMove(it) },
                            onToggleEngine = { analysisVm.toggleEngine() },
                            onToggleFlip = { analysisVm.toggleFlip() },
                            onJumpStart = { analysisVm.jumpToStart() },
                            onStepBack = { analysisVm.stepBack() },
                            onStepForward = { analysisVm.stepForward() },
                            onJumpEnd = { analysisVm.jumpToEnd() },
                            onJumpToPly = { analysisVm.jumpToPly(it) },
                            onSelectNode = { analysisVm.selectNode(it) },
                            onToggleAutoplay = { analysisVm.toggleAutoplay() },
                            onSetComment = { analysisVm.setComment(it) },
                            onToggleNag = { analysisVm.toggleNag(it) },
                            onToggleCircleSquare = { analysisVm.toggleUserCircle(it) },
                            onToggleUserArrow = { f, t -> analysisVm.toggleUserArrow(f, t) },
                            onClearAnnotations = { analysisVm.clearUserAnnotations() },
                            onRunUciCommand = { cmd -> analysisVm.runUciCommand(cmd) },
                            onRunUciBenchmark = { analysisVm.runUciBenchmarkHere() },
                            onRestartUciEngine = { analysisVm.restartUciEngine() },
                            onSaveCurrentAsPuzzle = { t, sol, th, r, h ->
                                analysisVm.saveCurrentPositionAsPuzzle(t, sol, th, r, h)
                            },
                            onRunFullReview = { analysisVm.runFullGameReview() },
                            onFetchExplorer = { analysisVm.fetchOnlineExplorer(it) },
                            onQueryTablebase = { analysisVm.queryEndgameTablebase() },
                            onLoadFen = { analysisVm.loadFen(it) },
                            onImportPgn = { analysisVm.importPgn(it) },
                            onExportPgn = { analysisVm.exportPgn() },
                            onSaveStudy = { t, d -> analysisVm.saveAsLocalStudy(t, d) },
                            onPublishStudyCloud = { t, d, pgn -> onlineVm?.publishStudy(t, d, pgn) },
                            onLoadSavedGame = { analysisVm.loadSavedGame(it) },
                            onToggleFavoriteGame = { g ->
                                scope.launch { localRepo.saveGame(g.copy(favorite = !g.favorite)) }
                            },
                            onDeleteGame = { id -> scope.launch { localRepo.deleteGame(id) } },
                            onCreateCollection = { name, desc ->
                                scope.launch { localRepo.saveCollection(CollectionEntity(UUID.randomUUID().toString(), name, desc)) }
                            },
                            onPlayFromPosition = { fen ->
                                localGameVm.startNewGame(
                                    mode = PlayMode.VS_BOT,
                                    variant = ChessVariant.STANDARD,
                                    timeControl = localGameState.timeControl,
                                    humanColor = com.example.chess.PieceColor.WHITE,
                                    customInitialFen = fen,
                                )
                                selectedTab = 1
                            },
                            onShareText = ::shareText,
                        )
                        else -> CommunityHubScreen(
                            isOnline = isOnline,
                            currentUserId = onlineUser?.uid,
                            isAdmin = onlineIsAdmin,
                            settings = settings,
                            myProfile = myProfile,
                            leaderboardState = leaderboardState,
                            playerSearchResults = playerSearchResults,
                            tournamentsState = tournamentsState,
                            selectedTournamentEntries = tournamentEntries,
                            clubsState = clubsState,
                            teamsState = teamsState,
                            liveWatchState = liveWatchState,
                            friendshipsState = friendshipsState,
                            threadsState = threadsState,
                            directMessages = directMessages,
                            publicStudiesState = publicStudiesState,
                            broadcasts = if (onlineVm != null) onlineBroadcasts else offlineBroadcasts,
                            tvChannels = if (onlineVm != null) onlineTv else offlineTv,
                            externalPlayerSummary = if (onlineVm != null) onlineExtPlayer else offlineExtPlayer,
                            adminReports = adminReports,
                            onRequestSignIn = onSwitchToOnlineSignIn,
                            onOpenSettings = { showSettings = true },
                            onOpenNewsReader = { showNewsReader = true },
                            onSearchPlayers = { q -> onlineVm?.searchPlayers(q) },
                            onCreateTournament = { n, f, v, tc -> onlineVm?.createTournament(n, f, v, tc) },
                            onJoinTournament = { t -> onlineVm?.joinTournament(t) },
                            onSelectTournamentForStandings = { id -> onlineVm?.observeTournamentStandings(id) },
                            onCreateClub = { n, d, c -> onlineVm?.createClub(n, d, c) },
                            onJoinClub = { c -> onlineVm?.joinClub(c) },
                            onCreateTeam = { n, d -> onlineVm?.createTeam(n, d) },
                            onSpectateGame = { gId ->
                                onlineVm?.openOnlineGame(gId)
                                selectedTab = 1
                            },
                            onLoadBroadcastsAndTv = {
                                if (onlineVm != null) {
                                    onlineVm.loadBroadcastsAndTv()
                                } else {
                                    scope.launch {
                                        externalApis.fetchLichessBroadcasts().onSuccess { offlineBroadcasts = it }
                                        externalApis.fetchLichessTvChannels().onSuccess { offlineTv = it }
                                    }
                                }
                            },
                            onChallengeFriend = { uid, name, tc, v -> onlineVm?.challengeFriend(uid, name, tc, v) },
                            onAddFriend = { uid, name -> onlineVm?.addFriend(uid, name) },
                            onAcceptFriend = { id -> onlineVm?.acceptFriend(id) },
                            onBlockFriend = { id -> onlineVm?.blockFriend(id) },
                            onOpenMessageThread = { tid -> onlineVm?.openMessageThread(tid) },
                            onSendDirectMessage = { uid, txt -> onlineVm?.sendDirectMessage(uid, txt) },
                            onSubmitReport = { uid, cat, det -> onlineVm?.submitReport(uid, cat, det) },
                            onLoadAdminReports = { onlineVm?.loadAdminReports() },
                            onResolveReport = { rid, st -> onlineVm?.resolveReport(rid, st) },
                            onUpdateProfile = { name, bio, iso, fide, pub ->
                                scope.launch { settingsRepo.setCountryAndFide(iso, fide) }
                                onlineVm?.updateProfile(name, bio, iso, fide, pub)
                            },
                            onUpdateLocalIdentity = { name, bio ->
                                scope.launch {
                                    settingsRepo.setLocalIdentity(name, bio)
                                    snackbarHostState.showSnackbar("Saved local player profile")
                                }
                            },
                            onAvatarSelected = { avatarId ->
                                scope.launch { settingsRepo.setLocalAvatarId(avatarId) }
                            },
                            onLookupExternalPlayer = { platform, user ->
                                if (onlineVm != null) {
                                    onlineVm.lookupExternalPlayer(platform, user)
                                } else {
                                    scope.launch {
                                        val res = if (platform == "Lichess") {
                                            externalApis.fetchLichessProfileAndGames(user)
                                        } else {
                                            externalApis.fetchChessComProfileAndGames(user)
                                        }
                                        res.onSuccess { offlineExtPlayer = it }
                                            .onFailure { snackbarHostState.showSnackbar(it.localizedMessage ?: "Lookup failed") }
                                    }
                                }
                            },
                            onImportExternalPgn = { pgn ->
                                if (analysisVm.importPgn(pgn)) selectedTab = 3
                            },
                            onSignOut = onSignOut,
                            savedGames = savedGames,
                            onOpenSavedGame = { game ->
                                analysisVm.loadSavedGame(game)
                                selectedTab = 3
                            },
                        )
                    }
                }
            }
        }
    }
}

private inline fun <reified VM : ViewModel> simpleVmFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
