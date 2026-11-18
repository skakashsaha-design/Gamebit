package com.example

import android.content.Context
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
import com.example.audio.SoundAndHapticsManager
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
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
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
    val localRepo = remember { LocalChessRepository(ChessHubDatabase.getInstance(appContext).chessHubDao()) }
    val soundManager = remember { SoundAndHapticsManager(appContext) }
    val externalApis = remember { ExternalChessApis() }

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
        factory = simpleVmFactory { LocalGameViewModel(localRepo, soundManager) },
    )
    val analysisVm: AnalysisAndReviewViewModel = viewModel(
        factory = simpleVmFactory { AnalysisAndReviewViewModel(localRepo, externalApis) },
    )
    val trainingVm: TrainingViewModel = viewModel(
        factory = simpleVmFactory { TrainingViewModel(localRepo, settingsRepo, externalApis) },
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

    LaunchedEffect(settings) {
        localGameVm.updateSettings(settings)
        analysisVm.updateSettings(settings)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

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
    val coordState by trainingVm.coordState.collectAsStateWithLifecycle()
    val puzzleAttempts by trainingVm.puzzleAttempts.collectAsStateWithLifecycle()
    val lessonProgress by trainingVm.lessonProgress.collectAsStateWithLifecycle()

    val savedGames by localRepo.observeGames().collectAsStateWithLifecycle(initialValue = emptyList())
    val studies by localRepo.observeStudies().collectAsStateWithLifecycle(initialValue = emptyList())
    val collections by localRepo.observeCollections().collectAsStateWithLifecycle(initialValue = emptyList())

    // Offline public external watch fallback state (zero Firestore)
    var offlineBroadcasts by remember { mutableStateOf<List<ExternalBroadcastItem>>(emptyList()) }
    var offlineTv by remember { mutableStateOf<List<ExternalTvChannel>>(emptyList()) }
    var offlineExtPlayer by remember { mutableStateOf<ExternalPlayerSummary?>(null) }

    // Online state subscriptions (when onlineVm != null)
    val onlineStatusMsg by (onlineVm?.statusMessage ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    val onlineIsAdmin by (onlineVm?.isAdmin ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsStateWithLifecycle()
    val myProfileState by (onlineVm?.myProfile ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val myProfile = (myProfileState as? UiState.Success)?.data
    val searchingTicketId by (onlineVm?.searchingTicketId ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    val activeOnlineGame by (onlineVm?.activeGameDoc ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    val onlineChat by (onlineVm?.activeGameChat ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val tournamentsState by (onlineVm?.tournaments ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val tournamentEntries by (onlineVm?.selectedTournamentEntries ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val clubsState by (onlineVm?.clubs ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val teamsState by (onlineVm?.teams ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val liveWatchState by (onlineVm?.liveWatchGames ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val leaderboardState by (onlineVm?.leaderboard ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val friendshipsState by (onlineVm?.friendships ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val threadsState by (onlineVm?.messageThreads ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val directMessages by (onlineVm?.directMessages ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val publicStudiesState by (onlineVm?.publicStudies ?: kotlinx.coroutines.flow.MutableStateFlow(UiState.Loading)).collectAsStateWithLifecycle()
    val onlineBroadcasts by (onlineVm?.broadcasts ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val onlineTv by (onlineVm?.tvChannels ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val onlineExtPlayer by (onlineVm?.externalPlayerSummary ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    val adminReports by (onlineVm?.adminReports ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()

    LaunchedEffect(onlineStatusMsg) {
        val msg = onlineStatusMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onlineVm?.clearStatusMessage()
    }

    LaunchedEffect(activeOnlineGame?.id) {
        if (activeOnlineGame != null) selectedTab = 1
    }

    BackHandler(enabled = showSettings || selectedTab != 0) {
        if (showSettings) showSettings = false else selectedTab = 0
    }

    if (showSettings) {
        SettingsAndAboutScreen(
            settings = settings,
            isLowRamDevice = settingsRepo.isDeviceLowRam,
            onBack = { showSettings = false },
            onDarkThemeChange = { scope.launch { settingsRepo.setDarkTheme(it) } },
            onHighContrastChange = { scope.launch { settingsRepo.setHighContrast(it) } },
            onBoardThemeChange = { scope.launch { settingsRepo.setBoardTheme(it) } },
            onShowCoordsChange = { scope.launch { settingsRepo.setShowCoordinates(it) } },
            onShowLegalChange = { scope.launch { settingsRepo.setShowLegalMoves(it) } },
            onLowMemoryChange = { scope.launch { settingsRepo.setLowMemoryMode(it) } },
            onAutoQueenChange = { scope.launch { settingsRepo.setAutoQueen(it) } },
            onConfirmMoveChange = { scope.launch { settingsRepo.setConfirmMoves(it) } },
            onPremoveChange = { scope.launch { settingsRepo.setPremoveEnabled(it) } },
            onSoundChange = { scope.launch { settingsRepo.setSoundEnabled(it) } },
            onSoundVolumeChange = { scope.launch { settingsRepo.setSoundVolume(it) } },
            onHapticsChange = { scope.launch { settingsRepo.setHapticsEnabled(it) } },
            onEvalBarChange = { scope.launch { settingsRepo.setShowEvalBar(it) } },
            onThreatArrowChange = { scope.launch { settingsRepo.setShowThreatArrow(it) } },
            onAllowChatChange = { scope.launch { settingsRepo.setAllowChatInGames(it) } },
            onFontScaleChange = { scope.launch { settingsRepo.setFontScale(it) } },
            onLanguageChange = { scope.launch { settingsRepo.setLanguage(it) } },
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
                            searchingTicketId = searchingTicketId,
                            activeOnlineGame = activeOnlineGame,
                            onlineChatMessages = onlineChat,
                            onLocalMove = { localGameVm.onUserMove(it) },
                            onQueuePremove = { localGameVm.queuePremove(it) },
                            onClearPremoves = { localGameVm.clearPremoves() },
                            onConfirmMove = { localGameVm.confirmPendingMove() },
                            onCancelConfirmMove = { localGameVm.cancelPendingMove() },
                            onSelectDropPiece = { localGameVm.selectDropPiece(it) },
                            onStartNewLocalGame = { mode, variant, tc, color, bot ->
                                localGameVm.startNewGame(mode, variant, tc, color, bot)
                            },
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
                            onCloseOnlineGame = { onlineVm?.closeActiveOnlineGame() },
                            onRequestOnlineSignIn = onSwitchToOnlineSignIn,
                        )
                        2 -> LearnScreen(
                            settings = settings,
                            puzzleState = puzzleState,
                            puzzleAttempts = puzzleAttempts,
                            lessonProgress = lessonProgress,
                            coordState = coordState,
                            onSelectPuzzleMode = { m, th -> trainingVm.selectPuzzleMode(m, th) },
                            onSelectTheme = { th -> trainingVm.selectThemeFilter(th) },
                            onSubmitPuzzleMove = { m -> trainingVm.submitPuzzleMove(m) },
                            onRetryPuzzle = { trainingVm.retryPuzzle() },
                            onNextPuzzle = { trainingVm.nextPuzzle() },
                            onRevealHint = { trainingVm.revealHint() },
                            onCompleteLessonStep = { lesson, step -> trainingVm.markLessonStepCompleted(lesson, step) },
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
