package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.AiDifficulty
import com.example.chess.BoardArrow
import com.example.chess.BotProfile
import com.example.chess.ChessBoardState
import com.example.chess.ChessHubBots
import com.example.chess.ChessVariant
import com.example.chess.ClockType
import com.example.chess.Glicko2RatingSystem
import com.example.chess.GlickoRating
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveQuality
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.ReviewedMove
import com.example.chess.TimeControl
import com.example.data.local.AnimationMode
import com.example.data.local.AppSettings
import com.example.data.remote.GameChatMessageDoc
import com.example.data.remote.OnlineGameDoc
import com.example.data.remote.PublicProfileDoc
import com.example.data.remote.RatingDeltaResult
import com.example.data.remote.WebSocketConnectionState
import com.example.ui.components.BotAvatarView
import com.example.ui.components.ChessBoardView
import com.example.ui.components.GamebitDialog
import com.example.ui.components.MoveExplanationDialog
import com.example.ui.components.MoveQualityBadge
import com.example.ui.components.PieceMiniIcon
import com.example.ui.components.PlayerAvatarView
import com.example.ui.components.PostGameComparisonDialog
import com.example.ui.theme.ChessBoardThemes
import com.example.viewmodel.LocalGameUiState
import com.example.viewmodel.PlayMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayScreen(
    localState: LocalGameUiState,
    settings: AppSettings,
    isOnline: Boolean,
    currentUserId: String?,
    myProfile: PublicProfileDoc?,
    searchingTicketId: String?,
    activeOnlineGame: OnlineGameDoc?,
    onlineChatMessages: List<GameChatMessageDoc>,
    onlineRatingDelta: RatingDeltaResult?,
    onlineConnectionState: WebSocketConnectionState = WebSocketConnectionState.DISCONNECTED,
    onReconnectOnline: () -> Unit = {},
    onLocalMove: (Move) -> Unit,
    onQueuePremove: (Move) -> Unit,
    onClearPremoves: () -> Unit,
    onConfirmMove: () -> Unit,
    onCancelConfirmMove: () -> Unit,
    onSelectDropPiece: (PieceType?) -> Unit,
    onStartNewLocalGame: (PlayMode, ChessVariant, TimeControl, PieceColor, BotProfile, BotProfile) -> Unit,
    onToggleBotVsBotPause: () -> Unit,
    onStepBotVsBotNext: () -> Unit,
    onToggleFlip: () -> Unit,
    onToggleClockPause: () -> Unit,
    onTakeback: () -> Unit,
    onRequestHint: () -> Unit,
    onResignLocal: () -> Unit,
    onAgreeDrawLocal: () -> Unit,
    onAnalyzePgn: (String) -> Unit,
    onShareText: (String, String) -> Unit,
    onStartMatchmaking: (ChessVariant, TimeControl, Boolean) -> Unit,
    onCancelMatchmaking: () -> Unit,
    onOnlineMove: (Move) -> Unit,
    onResignOnline: () -> Unit,
    onOfferOnlineDraw: () -> Unit,
    onSendOnlineChat: (String) -> Unit,
    onCloseOnlineGame: () -> Unit,
    onRequestOnlineSignIn: () -> Unit,
    onJumpToPly: (Int) -> Unit = {},
    onStepFirstMove: () -> Unit = {},
    onStepPreviousMove: () -> Unit = {},
    onStepNextMove: () -> Unit = {},
    onStepLatestMove: () -> Unit = {},
    onExportLocalPgn: () -> String = { "" },
    onImportLocalPgn: (String) -> Boolean = { false },
    onToggleMoveAnimation: (Boolean) -> Unit = {},
    onToggleRatedGame: (Boolean) -> Unit = {},
    onToggleMoveFeedback: (Boolean) -> Unit = {},
    onSaveGameSnapshot: () -> Unit = {},
    onSpeakBotDialogue: () -> Unit = {},
) {
    var selectedModeTab by remember(activeOnlineGame?.id) {
        mutableIntStateOf(if (activeOnlineGame != null) 1 else 0)
    }
    var showSetupSheet by remember { mutableStateOf(false) }

    val boardTheme = remember(settings.boardThemeId, settings.highContrast) {
        if (settings.highContrast) ChessBoardThemes.last()
        else ChessBoardThemes.firstOrNull { it.id == settings.boardThemeId } ?: ChessBoardThemes.first()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedModeTab) {
            Tab(
                selected = selectedModeTab == 0,
                onClick = { selectedModeTab = 0 },
                text = {
                    Text(
                        when (localState.mode) {
                            PlayMode.VS_BOT -> "Play vs Bot"
                            PlayMode.BOT_VS_BOT -> "Bot vs Bot"
                            PlayMode.PASS_AND_PLAY -> "Pass & Play (OTB)"
                        },
                    )
                },
                modifier = Modifier.testTag("tab_local_play"),
            )
            Tab(
                selected = selectedModeTab == 1,
                onClick = { selectedModeTab = 1 },
                text = { Text(if (activeOnlineGame != null) "Live Online" else "Online Matchmaking") },
                modifier = Modifier.testTag("tab_online_play"),
            )
        }

        if (selectedModeTab == 0) {
            LocalBoardContent(
                state = localState,
                settings = settings,
                boardTheme = boardTheme,
                onLocalMove = onLocalMove,
                onQueuePremove = onQueuePremove,
                onClearPremoves = onClearPremoves,
                onConfirmMove = onConfirmMove,
                onCancelConfirmMove = onCancelConfirmMove,
                onSelectDropPiece = onSelectDropPiece,
                onOpenSetup = { showSetupSheet = true },
                onToggleBotVsBotPause = onToggleBotVsBotPause,
                onStepBotVsBotNext = onStepBotVsBotNext,
                onToggleFlip = onToggleFlip,
                onToggleClockPause = onToggleClockPause,
                onTakeback = onTakeback,
                onRequestHint = onRequestHint,
                onResign = onResignLocal,
                onAgreeDraw = onAgreeDrawLocal,
                onRematch = {
                    onStartNewLocalGame(
                        localState.mode,
                        localState.variant,
                        localState.timeControl,
                        localState.humanColor,
                        localState.bot,
                        localState.botBlack,
                    )
                },
                onAnalyzePgn = onAnalyzePgn,
                onShareText = onShareText,
                onJumpToPly = onJumpToPly,
                onStepFirstMove = onStepFirstMove,
                onStepPreviousMove = onStepPreviousMove,
                onStepNextMove = onStepNextMove,
                onStepLatestMove = onStepLatestMove,
                onExportLocalPgn = onExportLocalPgn,
                onImportLocalPgn = onImportLocalPgn,
                onToggleMoveAnimation = onToggleMoveAnimation,
                onToggleMoveFeedback = onToggleMoveFeedback,
                onSaveGameSnapshot = onSaveGameSnapshot,
                onSpeakBotDialogue = onSpeakBotDialogue,
            )
        } else {
            OnlinePlayContent(
                isOnline = isOnline,
                currentUserId = currentUserId,
                myProfile = myProfile,
                settings = settings,
                boardTheme = boardTheme,
                searchingTicketId = searchingTicketId,
                activeGame = activeOnlineGame,
                chatMessages = onlineChatMessages,
                ratingDelta = onlineRatingDelta,
                connectionState = onlineConnectionState,
                onReconnectOnline = onReconnectOnline,
                onStartMatchmaking = onStartMatchmaking,
                onCancelMatchmaking = onCancelMatchmaking,
                onOnlineMove = onOnlineMove,
                onResignOnline = onResignOnline,
                onOfferOnlineDraw = onOfferOnlineDraw,
                onSendChat = onSendOnlineChat,
                onCloseGame = onCloseOnlineGame,
                onAnalyzePgn = onAnalyzePgn,
                onRequestSignIn = onRequestOnlineSignIn,
            )
        }
    }

    if (showSetupSheet) {
        NewLocalGameBottomSheet(
            currentState = localState,
            onDismiss = { showSetupSheet = false },
            onStart = { mode, variant, tc, color, bot, botBlack, rated ->
                showSetupSheet = false
                onToggleRatedGame(rated)
                onStartNewLocalGame(mode, variant, tc, color, bot, botBlack)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalBoardContent(
    state: LocalGameUiState,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onLocalMove: (Move) -> Unit,
    onQueuePremove: (Move) -> Unit,
    onClearPremoves: () -> Unit,
    onConfirmMove: () -> Unit,
    onCancelConfirmMove: () -> Unit,
    onSelectDropPiece: (PieceType?) -> Unit,
    onOpenSetup: () -> Unit,
    onToggleBotVsBotPause: () -> Unit,
    onStepBotVsBotNext: () -> Unit,
    onToggleFlip: () -> Unit,
    onToggleClockPause: () -> Unit,
    onTakeback: () -> Unit,
    onRequestHint: () -> Unit,
    onResign: () -> Unit,
    onAgreeDraw: () -> Unit,
    onRematch: () -> Unit,
    onAnalyzePgn: (String) -> Unit,
    onShareText: (String, String) -> Unit,
    onJumpToPly: (Int) -> Unit,
    onStepFirstMove: () -> Unit,
    onStepPreviousMove: () -> Unit,
    onStepNextMove: () -> Unit,
    onStepLatestMove: () -> Unit,
    onExportLocalPgn: () -> String,
    onImportLocalPgn: (String) -> Boolean,
    onToggleMoveAnimation: (Boolean) -> Unit,
    onToggleMoveFeedback: (Boolean) -> Unit,
    onSaveGameSnapshot: () -> Unit,
    onSpeakBotDialogue: () -> Unit,
) {
    var dismissResultBanner by remember(state.gameId) { mutableStateOf(false) }
    var showPgnDialog by remember { mutableStateOf(false) }
    var inspectedMove by remember { mutableStateOf<ReviewedMove?>(null) }

    val topColor = if (state.flipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val modeBadge = when (state.mode) {
                        PlayMode.VS_BOT -> " • ${state.aiDifficulty.displayName} AI" + if (state.ratedGame) " (Rated)" else " (Casual)"
                        PlayMode.BOT_VS_BOT -> " • Bot vs Bot"
                        PlayMode.PASS_AND_PLAY -> " • 2P OTB"
                    }
                    Text(
                        text = "${state.variant.displayName} • ${state.timeControl.label}$modeBadge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val openText = state.openingMatch?.let { "${it.name} (${it.eco})" } ?: "Ready"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.isCurrentMoveBook) {
                            MoveQualityBadge(quality = MoveQuality.BOOK, compact = true)
                        }
                        Text(
                            text = openText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onOpenSetup,
                    modifier = Modifier.testTag("new_game_setup_btn"),
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Game")
                }
            }
        }

        // Top Player Bar + Clock
        item {
            PlayerClockBar(
                name = playerLabelFor(state, topColor),
                subtitle = playerSubtitleFor(state, topColor),
                side = topColor,
                pieceStyleId = settings.pieceStyleId,
                bot = botForColor(state, topColor),
                playerAvatarId = if (botForColor(state, topColor) == null && (state.mode != PlayMode.PASS_AND_PLAY || topColor == PieceColor.WHITE)) settings.localAvatarId else null,
                timeMs = if (topColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs,
                initialTimeMs = if (state.timeControl.clockType == ClockType.UNLIMITED) 0L else state.timeControl.initialSeconds * 1000L,
                clockLabel = state.timeControl.label,
                isTurn = !state.gameStatus.isTerminal && state.boardState.sideToMove == topColor,
                isPaused = state.isClockPaused,
                isUnlimited = state.timeControl.clockType == ClockType.UNLIMITED,
                isThinking = state.isBotThinking && (
                    (state.mode == PlayMode.VS_BOT && topColor != state.humanColor) ||
                        (state.mode == PlayMode.BOT_VS_BOT && state.boardState.sideToMove == topColor)
                    ),
                checksGiven = if (state.variant == ChessVariant.THREE_CHECK) {
                    if (topColor == PieceColor.WHITE) (3 - state.boardState.blackChecksRemaining) else (3 - state.boardState.whiteChecksRemaining)
                } else null,
            )
        }

        // Reviewing history indicator banner when browsing previous moves
        if (!state.isViewingLive) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("history_review_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Reviewing move ${state.viewingPly} of ${state.moveHistorySan.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(onClick = onStepLatestMove) {
                            Text("Jump to Live")
                        }
                    }
                }
            }
        }

        // Chessboard (displays historical position when navigating move history)
        item {
            val arrows = remember(state.hintMove, state.isViewingLive) {
                if (state.isViewingLive) {
                    state.hintMove?.let { listOf(BoardArrow(it.from, it.to, 0xCC10B981)) } ?: emptyList()
                } else emptyList()
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ChessBoardView(
                    state = state.displayedBoardState,
                    legalMoves = if (state.isViewingLive) state.legalMoves else emptyList(),
                    theme = boardTheme,
                    modifier = Modifier.widthIn(max = 520.dp),
                    flipped = state.flipped,
                    interactive = state.isViewingLive && !state.gameStatus.isTerminal && state.mode != PlayMode.BOT_VS_BOT,
                    allowBothSides = state.mode == PlayMode.PASS_AND_PLAY,
                    humanSide = state.humanColor,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves && state.isViewingLive,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    pieceStyleId = settings.pieceStyleId,
                    moveAnimation = settings.moveAnimation,
                    reducedAnimations = settings.reducedAnimations,
                    animationMode = settings.animationMode,
                    animationSpeed = settings.animationSpeed,
                    lastMove = state.displayedLastMove,
                    premoveQueue = if (state.isViewingLive) state.premoveQueue else emptyList(),
                    arrows = arrows,
                    selectedDropPiece = if (state.isViewingLive) state.selectedDropPiece else null,
                    onMoveSelected = onLocalMove,
                    onPremoveQueued = onQueuePremove,
                )
            }
        }

        // Crazyhouse pocket bar when playing Crazyhouse
        if (state.variant == ChessVariant.CRAZYHOUSE && state.isViewingLive) {
            item {
                val pocket = if (state.boardState.sideToMove == PieceColor.WHITE) {
                    state.boardState.whitePocket
                } else {
                    state.boardState.blackPocket
                }
                if (pocket.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pocket:", style = MaterialTheme.typography.labelMedium)
                        pocket.entries.forEach { (pt, count) ->
                            FilterChip(
                                selected = state.selectedDropPiece == pt,
                                onClick = { onSelectDropPiece(pt) },
                                label = { Text("${pt.sanLetter.ifBlank { "P" }} x$count") },
                            )
                        }
                    }
                }
            }
        }

        // Bottom Player Bar + Clock
        item {
            PlayerClockBar(
                name = playerLabelFor(state, bottomColor),
                subtitle = playerSubtitleFor(state, bottomColor),
                side = bottomColor,
                pieceStyleId = settings.pieceStyleId,
                bot = botForColor(state, bottomColor),
                playerAvatarId = if (botForColor(state, bottomColor) == null && (state.mode != PlayMode.PASS_AND_PLAY || bottomColor == PieceColor.WHITE)) settings.localAvatarId else null,
                timeMs = if (bottomColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs,
                initialTimeMs = if (state.timeControl.clockType == ClockType.UNLIMITED) 0L else state.timeControl.initialSeconds * 1000L,
                clockLabel = state.timeControl.label,
                isTurn = !state.gameStatus.isTerminal && state.boardState.sideToMove == bottomColor,
                isPaused = state.isClockPaused,
                isUnlimited = state.timeControl.clockType == ClockType.UNLIMITED,
                isThinking = state.isBotThinking && (
                    (state.mode == PlayMode.VS_BOT && bottomColor != state.humanColor) ||
                        (state.mode == PlayMode.BOT_VS_BOT && state.boardState.sideToMove == bottomColor)
                    ),
                checksGiven = if (state.variant == ChessVariant.THREE_CHECK) {
                    if (bottomColor == PieceColor.WHITE) (3 - state.boardState.blackChecksRemaining) else (3 - state.boardState.whiteChecksRemaining)
                } else null,
            )
        }

        // Move confirmation bar / Premove indicator
        if (state.pendingConfirmMove != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Confirm move ${state.pendingConfirmMove.uci}?", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCancelConfirmMove) { Text("Cancel") }
                            Button(onClick = onConfirmMove, modifier = Modifier.testTag("confirm_move_btn")) { Text("Confirm") }
                        }
                    }
                }
            }
        }
        if (state.premoveQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Premoves queued: ${state.premoveQueue.joinToString { it.uci }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    TextButton(onClick = onClearPremoves) { Text("Clear Premoves") }
                }
            }
        }

        if (settings.moveFeedbackEnabled && state.moveQualities.isNotEmpty()) {
            val latestQuality = state.moveQualities.lastOrNull()
            val latestSan = state.moveHistorySan.lastOrNull()
            val latestExplanation = state.moveExplanations.lastOrNull().orEmpty()
            val latestReviewed = state.reviewedMoves.lastOrNull()
            if (latestQuality != null && latestSan != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("live_move_feedback_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MoveQualityBadge(quality = latestQuality, compact = true)
                                Column {
                                    Text(
                                        text = "$latestSan • ${latestQuality.label}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (latestExplanation.isNotBlank()) {
                                        Text(
                                            text = latestExplanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            if (latestReviewed != null) {
                                TextButton(onClick = { inspectedMove = latestReviewed }) {
                                    Text("Why?")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.coachMessage.isNotBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.coachMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        if (state.botDialogueText.isNotBlank() && state.mode != PlayMode.PASS_AND_PLAY) {
            item {
                val speakerBot = if (state.mode == PlayMode.BOT_VS_BOT && state.moveHistorySan.isNotEmpty() && state.boardState.sideToMove == PieceColor.WHITE) {
                    state.botBlack
                } else {
                    state.bot
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(speakerBot.avatarSpec.accentHex).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("bot_dialogue_banner"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BotAvatarView(
                            bot = speakerBot,
                            size = 40.dp,
                            pieceStyleId = settings.pieceStyleId,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = speakerBot.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = "${speakerBot.voiceProfile.voiceStyle.label} Voice",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                            Text(
                                text = "\"${state.botDialogueText}\"",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(
                            onClick = onSpeakBotDialogue,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("bot_speak_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak bot dialogue",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        state.gambitMatch?.let { g ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${g.name} (${g.eco}) • Gambit ${g.status}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(g.recommendedIdeas, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        state.endgameClassification?.let { eg ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Endgame: ${eg.category} — ${eg.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        // Action Controls Row
        if (state.mode == PlayMode.BOT_VS_BOT && !state.gameStatus.isTerminal) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onToggleBotVsBotPause,
                        modifier = Modifier.weight(1f).testTag("bot_vs_bot_pause_btn"),
                    ) {
                        Text(if (state.botVsBotPaused) "Resume Match" else "Pause Match")
                    }
                    OutlinedButton(
                        onClick = onStepBotVsBotNext,
                        enabled = !state.isBotThinking,
                        modifier = Modifier.weight(1f).testTag("bot_vs_bot_step_btn"),
                    ) {
                        Text("Step Next Move")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = onToggleFlip, modifier = Modifier.testTag("flip_board_btn")) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip board")
                }
                IconButton(
                    onClick = onTakeback,
                    enabled = !state.gameStatus.isTerminal && state.moveHistorySan.isNotEmpty() && state.mode != PlayMode.BOT_VS_BOT,
                    modifier = Modifier.testTag("takeback_btn"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Takeback")
                }
                IconButton(
                    onClick = onRequestHint,
                    enabled = !state.gameStatus.isTerminal,
                    modifier = Modifier.testTag("hint_btn"),
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Hint")
                }
                if (state.mode == PlayMode.PASS_AND_PLAY && state.timeControl.clockType != ClockType.UNLIMITED) {
                    IconButton(onClick = onToggleClockPause) {
                        Icon(
                            if (state.isClockPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isClockPaused) "Resume clock" else "Pause clock",
                        )
                    }
                }
                IconButton(
                    onClick = onAgreeDraw,
                    enabled = !state.gameStatus.isTerminal,
                    modifier = Modifier.testTag("draw_btn"),
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = "Draw")
                }
                IconButton(
                    onClick = onResign,
                    enabled = !state.gameStatus.isTerminal,
                    modifier = Modifier.testTag("resign_btn"),
                ) {
                    Icon(Icons.Default.Flag, contentDescription = "Resign")
                }
                val slideEnabled = settings.moveAnimation && !settings.reducedAnimations && settings.animationMode != AnimationMode.OFF
                IconButton(
                    onClick = { onToggleMoveAnimation(!slideEnabled) },
                    modifier = Modifier.testTag("play_slide_anim_btn"),
                ) {
                    Icon(
                        Icons.Default.Animation,
                        contentDescription = if (slideEnabled) "Disable piece slide animation" else "Enable piece slide animation",
                        tint = if (slideEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { onToggleMoveFeedback(!settings.moveFeedbackEnabled) },
                    modifier = Modifier.testTag("play_feedback_toggle_btn"),
                ) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = if (settings.moveFeedbackEnabled) "Disable move feedback" else "Enable move feedback",
                        tint = if (settings.moveFeedbackEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Terminal Result Summary Card below board
        if (state.gameStatus.isTerminal) {
            item {
                val winnerText = when (state.winner) {
                    PieceColor.WHITE -> "White Wins"
                    PieceColor.BLACK -> "Black Wins"
                    null -> "Draw"
                }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("game_result_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "$winnerText — ${state.gameStatus.description}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { dismissResultBanner = false }, modifier = Modifier.testTag("open_post_game_summary_btn")) {
                                Text("Full Summary")
                            }
                            FilledTonalButton(onClick = onRematch, modifier = Modifier.testTag("inline_rematch_btn")) {
                                Text("Rematch")
                            }
                            FilledTonalButton(
                                onClick = {
                                    val fullPgn = onExportLocalPgn().ifBlank {
                                        buildString {
                                            append("[Event \"Gamebit Game\"]\n")
                                            append("[White \"${playerLabelFor(state, PieceColor.WHITE)}\"]\n")
                                            append("[Black \"${playerLabelFor(state, PieceColor.BLACK)}\"]\n\n")
                                            state.moveHistorySan.chunked(2).forEachIndexed { idx, pair ->
                                                append("${idx + 1}. ${pair[0]} ")
                                                if (pair.size > 1) append("${pair[1]} ")
                                            }
                                        }
                                    }
                                    onAnalyzePgn(fullPgn)
                                },
                                modifier = Modifier.testTag("inline_review_game_btn"),
                            ) {
                                Text("Review & Analyze")
                            }
                            OutlinedButton(
                                onClick = { showPgnDialog = true },
                                modifier = Modifier.testTag("inline_result_pgn_btn"),
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PGN")
                            }
                            OutlinedButton(
                                onClick = { onShareText("Gamebit FEN", state.displayedBoardState.toFen()) },
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share FEN")
                            }
                        }
                    }
                }
            }
        }

        // Scrollable Move History & Navigation + PGN Panel
        item {
            MoveHistoryPanel(
                moveHistorySan = state.moveHistorySan,
                viewingPly = state.viewingPly,
                isViewingLive = state.isViewingLive,
                moveQualities = state.moveQualities,
                reviewedMoves = state.reviewedMoves,
                showFeedbackBadges = settings.moveFeedbackEnabled,
                onJumpToPly = onJumpToPly,
                onStepFirst = onStepFirstMove,
                onStepPrev = onStepPreviousMove,
                onStepNext = onStepNextMove,
                onStepLatest = onStepLatestMove,
                onInspectMove = { inspectedMove = it },
                onOpenPgnSheet = { showPgnDialog = true },
            )
            Spacer(Modifier.height(14.dp))
        }
    }

    if (state.gameStatus.isTerminal && !dismissResultBanner) {
        val fullPgn = onExportLocalPgn().ifBlank {
            buildString {
                append("[Event \"Gamebit Local Game\"]\n")
                append("[White \"${playerLabelFor(state, PieceColor.WHITE)}\"]\n")
                append("[Black \"${playerLabelFor(state, PieceColor.BLACK)}\"]\n\n")
                state.moveHistorySan.chunked(2).forEachIndexed { idx, pair ->
                    append("${idx + 1}. ${pair[0]} ")
                    if (pair.size > 1) append("${pair[1]} ")
                }
            }
        }
        PostGameComparisonDialog(
            gameStatus = state.gameStatus,
            winner = state.winner,
            whiteName = playerLabelFor(state, PieceColor.WHITE),
            blackName = playerLabelFor(state, PieceColor.BLACK),
            eloDelta = state.lastEloDelta,
            report = state.postGameSummary,
            reviewedMoves = state.reviewedMoves,
            isGeneratingSummary = state.isGeneratingSummary,
            reducedAnimations = settings.reducedAnimations || settings.lowMemoryMode || settings.animationMode != AnimationMode.FULL,
            savedToArchive = state.savedToArchive,
            onReviewGame = {
                dismissResultBanner = true
                onAnalyzePgn(fullPgn)
            },
            onAnalyze = {
                dismissResultBanner = true
                onAnalyzePgn(fullPgn)
            },
            onRematch = {
                dismissResultBanner = true
                onRematch()
            },
            onNewGame = {
                dismissResultBanner = true
                onOpenSetup()
            },
            onShareGame = { onShareText("Gamebit PGN", fullPgn) },
            onSaveGame = onSaveGameSnapshot,
            onDismiss = { dismissResultBanner = true },
        )
    }

    inspectedMove?.let { rm ->
        MoveExplanationDialog(
            reviewedMove = rm,
            onDismiss = { inspectedMove = null },
        )
    }

    if (showPgnDialog) {
        LocalPgnDialog(
            currentPgn = onExportLocalPgn().ifBlank {
                buildString {
                    append("[Event \"Gamebit Local Game\"]\n")
                    append("[White \"${playerLabelFor(state, PieceColor.WHITE)}\"]\n")
                    append("[Black \"${playerLabelFor(state, PieceColor.BLACK)}\"]\n\n")
                    state.moveHistorySan.chunked(2).forEachIndexed { idx, pair ->
                        append("${idx + 1}. ${pair[0]} ")
                        if (pair.size > 1) append("${pair[1]} ")
                    }
                }
            },
            onDismiss = { showPgnDialog = false },
            onSharePgn = { pgn -> onShareText("Gamebit PGN", pgn) },
            onAnalyzePgn = { pgn ->
                showPgnDialog = false
                onAnalyzePgn(pgn)
            },
            onImportPgn = { pgn ->
                val ok = onImportLocalPgn(pgn)
                if (ok) showPgnDialog = false
                ok
            },
        )
    }
}

@Composable
private fun MoveHistoryPanel(
    moveHistorySan: List<String>,
    viewingPly: Int,
    isViewingLive: Boolean,
    moveQualities: List<MoveQuality?> = emptyList(),
    reviewedMoves: List<ReviewedMove> = emptyList(),
    showFeedbackBadges: Boolean = true,
    onJumpToPly: (Int) -> Unit,
    onStepFirst: () -> Unit,
    onStepPrev: () -> Unit,
    onStepNext: () -> Unit,
    onStepLatest: () -> Unit,
    onInspectMove: (ReviewedMove) -> Unit = {},
    onOpenPgnSheet: () -> Unit,
) {
    val pairs = remember(moveHistorySan) { moveHistorySan.chunked(2) }
    val listState = rememberLazyListState()
    val selectedReviewed = remember(reviewedMoves, viewingPly) {
        reviewedMoves.firstOrNull { it.ply == viewingPly }
    }

    LaunchedEffect(viewingPly, pairs.size) {
        if (pairs.isNotEmpty()) {
            val targetRow = if (viewingPly <= 0) 0 else ((viewingPly - 1) / 2).coerceIn(0, pairs.lastIndex)
            listState.animateScrollToItem(targetRow)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("move_history_panel"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Move History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isViewingLive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = if (moveHistorySan.isEmpty()) "Start" else "$viewingPly / ${moveHistorySan.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectedReviewed != null) {
                        TextButton(onClick = { onInspectMove(selectedReviewed) }) {
                            Text("Why?")
                        }
                    }
                    FilledTonalButton(
                        onClick = onOpenPgnSheet,
                        modifier = Modifier.height(36.dp).testTag("open_pgn_dialog_btn"),
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PGN", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (pairs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No moves yet — play a move or import PGN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp, max = 152.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("move_history_scroll_list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(pairs.size) { rowIdx ->
                        val pair = pairs[rowIdx]
                        val whitePly = rowIdx * 2 + 1
                        val blackPly = rowIdx * 2 + 2
                        val wQuality = if (showFeedbackBadges) moveQualities.getOrNull(whitePly - 1) else null
                        val bQuality = if (showFeedbackBadges) moveQualities.getOrNull(blackPly - 1) else null
                        val wReviewed = reviewedMoves.firstOrNull { it.ply == whitePly }
                        val bReviewed = reviewedMoves.firstOrNull { it.ply == blackPly }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (rowIdx % 2 == 1) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${rowIdx + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp),
                            )
                            MoveHistoryPlyPill(
                                san = pair[0],
                                quality = wQuality,
                                selected = viewingPly == whitePly,
                                onClick = {
                                    onJumpToPly(whitePly)
                                    if (viewingPly == whitePly && wReviewed != null) {
                                        onInspectMove(wReviewed)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("move_ply_$whitePly"),
                            )
                            Spacer(Modifier.width(4.dp))
                            if (pair.size > 1) {
                                MoveHistoryPlyPill(
                                    san = pair[1],
                                    quality = bQuality,
                                    selected = viewingPly == blackPly,
                                    onClick = {
                                        onJumpToPly(blackPly)
                                        if (viewingPly == blackPly && bReviewed != null) {
                                            onInspectMove(bReviewed)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("move_ply_$blackPly"),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onStepFirst,
                    enabled = viewingPly > 0,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("move_nav_first"),
                ) {
                    Icon(Icons.Default.FirstPage, contentDescription = "First position")
                }
                IconButton(
                    onClick = onStepPrev,
                    enabled = viewingPly > 0,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("move_nav_prev"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous move")
                }
                IconButton(
                    onClick = onStepNext,
                    enabled = viewingPly < moveHistorySan.size,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("move_nav_next"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next move")
                }
                IconButton(
                    onClick = onStepLatest,
                    enabled = viewingPly < moveHistorySan.size,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("move_nav_last"),
                ) {
                    Icon(Icons.Default.LastPage, contentDescription = "Latest move")
                }
            }
        }
    }
}

@Composable
private fun MoveHistoryPlyPill(
    san: String,
    quality: MoveQuality?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = san,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (quality != null) {
                MoveQualityBadge(quality = quality, compact = true)
            }
        }
    }
}

@Composable
private fun LocalPgnDialog(
    currentPgn: String,
    onDismiss: () -> Unit,
    onSharePgn: (String) -> Unit,
    onAnalyzePgn: (String) -> Unit,
    onImportPgn: (String) -> Boolean,
) {
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var pgnDraft by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    GamebitDialog(
        onDismissRequest = onDismiss,
        title = "PGN Export & Import",
        actions = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0; feedback = null }, text = { Text("Export PGN") })
            Tab(selected = tab == 1, onClick = { tab = 1; feedback = null }, text = { Text("Import PGN") })
        }
        if (tab == 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 210.dp),
            ) {
                Text(
                    text = currentPgn,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(10.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(currentPgn))
                        feedback = "PGN copied to clipboard."
                    },
                    modifier = Modifier.weight(1f).testTag("copy_pgn_btn"),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                OutlinedButton(
                    onClick = { onSharePgn(currentPgn) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
            FilledTonalButton(
                onClick = { onAnalyzePgn(currentPgn) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open in Deep Analysis")
            }
        } else {
            OutlinedTextField(
                value = pgnDraft,
                onValueChange = {
                    pgnDraft = it
                    feedback = null
                },
                label = { Text("Paste PGN moves or full headers") },
                placeholder = { Text("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 180.dp).testTag("pgn_import_input"),
            )
            TextButton(
                onClick = {
                    pgnDraft = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O"
                    feedback = null
                },
            ) {
                Text("Insert Sample Ruy Lopez PGN")
            }
            Button(
                onClick = {
                    if (!onImportPgn(pgnDraft)) {
                        feedback = "Could not parse PGN. Check move notation and try again."
                    }
                },
                enabled = pgnDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("load_pgn_into_board_btn"),
            ) {
                Text("Load PGN into Board")
            }
        }
        feedback?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PlayerClockBar(
    name: String,
    subtitle: String,
    side: PieceColor,
    pieceStyleId: String,
    bot: BotProfile? = null,
    playerAvatarId: String? = null,
    timeMs: Long,
    initialTimeMs: Long,
    clockLabel: String,
    isTurn: Boolean,
    isPaused: Boolean,
    isUnlimited: Boolean,
    isThinking: Boolean,
    checksGiven: Int?,
) {
    val isCriticalLow = !isUnlimited && timeMs in 1L..19_999L
    val isWarningLow = !isUnlimited && timeMs in 20_000L..29_999L

    val containerColor by animateColorAsState(
        targetValue = when {
            isTurn && isCriticalLow -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            isTurn -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "clock_card_bg",
    )
    val clockPillBg by animateColorAsState(
        targetValue = when {
            isCriticalLow -> MaterialTheme.colorScheme.error
            isTurn && isWarningLow -> Color(0xFFD97706)
            isTurn -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "clock_pill_bg",
    )
    val clockTextColor = when {
        isCriticalLow -> MaterialTheme.colorScheme.onError
        isTurn && isWarningLow -> Color.White
        isTurn -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progress = if (isUnlimited || initialTimeMs <= 0L) 1f else (timeMs.toFloat() / initialTimeMs.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("player_clock_${side.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = if (isTurn) 3.dp else 0.dp,
        border = BorderStroke(
            width = if (isTurn) 1.5.dp else 1.dp,
            color = when {
                isTurn && isCriticalLow -> MaterialTheme.colorScheme.error
                isTurn -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when {
                        bot != null -> {
                            BotAvatarView(
                                bot = bot,
                                size = 38.dp,
                                pieceStyleId = pieceStyleId,
                                cornerRadius = 11.dp,
                            )
                        }
                        playerAvatarId != null -> {
                            PlayerAvatarView(
                                avatarId = playerAvatarId,
                                side = side,
                                size = 38.dp,
                                pieceStyleId = pieceStyleId,
                                cornerRadius = 11.dp,
                            )
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (side == PieceColor.WHITE) Color(0xFFF3F4F6) else Color(0xFF1F2937),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PieceMiniIcon(
                                        piece = Piece(PieceType.KING, side),
                                        modifier = Modifier.size(26.dp),
                                        pieceStyleId = pieceStyleId,
                                    )
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isTurn) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isCriticalLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                )
                            }
                            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (isThinking) {
                                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isPaused) "$subtitle • Paused" else subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (checksGiven != null) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(
                                        text = "Checks $checksGiven/3",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                val clockText = if (isUnlimited) "∞" else formatClockMs(timeMs)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = clockPillBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = if (isUnlimited) "Unlimited clock $clockLabel" else "Clock $clockText",
                            tint = clockTextColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = clockText,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = clockTextColor,
                        )
                    }
                }
            }
            if (!isUnlimited) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = when {
                        isCriticalLow -> MaterialTheme.colorScheme.error
                        isWarningLow -> Color(0xFFD97706)
                        isTurn -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewLocalGameBottomSheet(
    currentState: LocalGameUiState,
    onDismiss: () -> Unit,
    onStart: (PlayMode, ChessVariant, TimeControl, PieceColor, BotProfile, BotProfile, Boolean) -> Unit,
) {
    var mode by remember { mutableStateOf(currentState.mode) }
    var variant by remember { mutableStateOf(currentState.variant) }
    var selectedPreset by remember { mutableStateOf(currentState.timeControl) }
    var useCustomClock by remember { mutableStateOf(false) }
    var customMin by remember { mutableFloatStateOf(5f) }
    var customInc by remember { mutableFloatStateOf(3f) }
    var customClockType by remember { mutableStateOf(ClockType.FISCHER) }
    var humanColor by remember { mutableStateOf(currentState.humanColor) }
    var bot by remember { mutableStateOf(currentState.bot) }
    var selectedDifficulty by remember { mutableStateOf(currentState.aiDifficulty) }
    var botBlack by remember { mutableStateOf(currentState.botBlack) }
    var ratedGame by remember { mutableStateOf(currentState.ratedGame) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("New Game Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mode == PlayMode.VS_BOT,
                        onClick = { mode = PlayMode.VS_BOT },
                        label = { Text("Play vs Bot") },
                    )
                    FilterChip(
                        selected = mode == PlayMode.PASS_AND_PLAY,
                        onClick = { mode = PlayMode.PASS_AND_PLAY },
                        label = { Text("Over the Board (2P)") },
                    )
                    FilterChip(
                        selected = mode == PlayMode.BOT_VS_BOT,
                        onClick = { mode = PlayMode.BOT_VS_BOT },
                        label = { Text("Bot vs Bot") },
                    )
                }
            }
            if (mode == PlayMode.VS_BOT || mode == PlayMode.BOT_VS_BOT) {
                if (mode == PlayMode.VS_BOT) {
                    item {
                        Text("AI Difficulty Level", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AiDifficulty.entries.forEach { level ->
                                FilterChip(
                                    selected = selectedDifficulty == level,
                                    onClick = {
                                        selectedDifficulty = level
                                        bot = level.toBotProfile()
                                    },
                                    label = { Text("${level.displayName} (~${level.approxElo})") },
                                    modifier = Modifier.testTag("ai_difficulty_${level.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        if (mode == PlayMode.BOT_VS_BOT) "White Bot (${ChessHubBots.ALL.size} Profiles)" else "Or Select Specific Bot (${ChessHubBots.ALL.size} Bots)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ChessHubBots.ALL, key = { it.id }) { b ->
                            BotSelectorChipCard(
                                bot = b,
                                selected = bot.id == b.id,
                                testTag = "bot_option_${b.id}",
                                onClick = {
                                    bot = b
                                    selectedDifficulty = AiDifficulty.fromBotProfile(b)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BotAvatarView(bot = bot, size = 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${bot.name} (${bot.approxStrengthLabel}) • ${bot.voiceProfile.voiceStyle.label} Voice",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${bot.styleTag} — ${bot.bio}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (mode == PlayMode.VS_BOT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            FilterChip(
                                selected = humanColor == PieceColor.WHITE,
                                onClick = { humanColor = PieceColor.WHITE },
                                label = { Text("Play White") },
                            )
                            FilterChip(
                                selected = humanColor == PieceColor.BLACK,
                                onClick = { humanColor = PieceColor.BLACK },
                                label = { Text("Play Black") },
                            )
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("Black Bot", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ChessHubBots.ALL, key = { "black_${it.id}" }) { b ->
                                BotSelectorChipCard(
                                    bot = b,
                                    selected = botBlack.id == b.id,
                                    testTag = "bot_black_option_${b.id}",
                                    onClick = { botBlack = b },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text("Chess Variant", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChessVariant.entries.forEach { v ->
                        FilterChip(
                            selected = variant == v,
                            onClick = { variant = v },
                            label = { Text(v.displayName) },
                        )
                    }
                }
                Text(variant.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Time Control", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Custom", style = MaterialTheme.typography.labelMedium)
                        Switch(checked = useCustomClock, onCheckedChange = { useCustomClock = it })
                    }
                }
                if (!useCustomClock) {
                    val grouped = listOf(
                        "Bullet" to TimeControl.PRESETS.filter { it.category == "bullet" },
                        "Blitz" to TimeControl.PRESETS.filter { it.category == "blitz" },
                        "Rapid" to TimeControl.PRESETS.filter { it.category == "rapid" },
                        "Classical & Other" to TimeControl.PRESETS.filter { it.category !in listOf("bullet", "blitz", "rapid") },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        grouped.forEach { (catTitle, list) ->
                            if (list.isNotEmpty()) {
                                Text(
                                    text = catTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    list.forEach { tc ->
                                        FilterChip(
                                            selected = selectedPreset.label == tc.label,
                                            onClick = { selectedPreset = tc },
                                            label = { Text(tc.label) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Minutes: ${customMin.roundToInt()}  |  Inc/Delay: ${customInc.roundToInt()}s")
                    Slider(value = customMin, onValueChange = { customMin = it }, valueRange = 1f..60f)
                    Slider(value = customInc, onValueChange = { customInc = it }, valueRange = 0f..30f)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ClockType.FISCHER, ClockType.BRONSTEIN, ClockType.SIMPLE_DELAY, ClockType.UNLIMITED).forEach { ct ->
                            FilterChip(
                                selected = customClockType == ct,
                                onClick = { customClockType = ct },
                                label = { Text(ct.label) },
                            )
                        }
                    }
                }
            }
            if (mode == PlayMode.VS_BOT) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rated Game (Bot)", style = MaterialTheme.typography.labelLarge)
                            Text("Updates your local Bullet/Blitz/Rapid/Classical Elo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = ratedGame,
                            onCheckedChange = { ratedGame = it },
                            modifier = Modifier.testTag("setup_rated_switch"),
                        )
                    }
                }
            }
            item {
                val chosenTc = if (useCustomClock) {
                    val m = customMin.roundToInt()
                    val inc = customInc.roundToInt()
                    val label = if (customClockType == ClockType.UNLIMITED) "Unlimited" else "$m+$inc"
                    TimeControl(
                        label = label,
                        initialSeconds = if (customClockType == ClockType.UNLIMITED) 0 else m * 60,
                        incrementSeconds = if (customClockType == ClockType.FISCHER) inc else 0,
                        delaySeconds = if (customClockType == ClockType.SIMPLE_DELAY || customClockType == ClockType.BRONSTEIN) inc else 0,
                        clockType = customClockType,
                    )
                } else selectedPreset
                Button(
                    onClick = { onStart(mode, variant, chosenTc, humanColor, bot, botBlack, ratedGame) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_configured_game_btn"),
                ) {
                    Text("Start Game")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlinePlayContent(
    isOnline: Boolean,
    currentUserId: String?,
    myProfile: PublicProfileDoc?,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    searchingTicketId: String?,
    activeGame: OnlineGameDoc?,
    chatMessages: List<GameChatMessageDoc>,
    ratingDelta: RatingDeltaResult?,
    connectionState: WebSocketConnectionState,
    onReconnectOnline: () -> Unit,
    onStartMatchmaking: (ChessVariant, TimeControl, Boolean) -> Unit,
    onCancelMatchmaking: () -> Unit,
    onOnlineMove: (Move) -> Unit,
    onResignOnline: () -> Unit,
    onOfferOnlineDraw: () -> Unit,
    onSendChat: (String) -> Unit,
    onCloseGame: () -> Unit,
    onAnalyzePgn: (String) -> Unit,
    onRequestSignIn: () -> Unit,
) {
    if (!isOnline || currentUserId == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Connect for Real-Time Online Chess", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Matchmaking, server-authoritative move validation, live clocks, Glicko-2 ratings, and game chat sync via HTTPS & WebSockets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestSignIn) { Text("Sign In Online") }
            }
        }
        return
    }

    if (activeGame == null) {
        var variant by remember { mutableStateOf(ChessVariant.STANDARD) }
        var tc by remember { mutableStateOf(TimeControl.PRESETS[3]) }
        var rated by remember { mutableStateOf(true) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Online Matchmaking Pool", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Server-authoritative pairing via REST & WebSocket (${connectionState.name.lowercase()}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (connectionState != WebSocketConnectionState.CONNECTED) {
                        TextButton(onClick = onReconnectOnline) { Text("Reconnect") }
                    }
                }
            }
            item {
                Text("Time Control", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        TimeControl.PRESETS[0],
                        TimeControl.PRESETS[3],
                        TimeControl.PRESETS[5],
                        TimeControl.PRESETS[6],
                        TimeControl.PRESETS[8],
                    ).forEach { preset ->
                        FilterChip(selected = tc.label == preset.label, onClick = { tc = preset }, label = { Text(preset.label) })
                    }
                }
            }
            item {
                Text("Variant", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ChessVariant.STANDARD, ChessVariant.CHESS960, ChessVariant.THREE_CHECK, ChessVariant.KING_OF_THE_HILL).forEach { v ->
                        FilterChip(selected = variant == v, onClick = { variant = v }, label = { Text(v.displayName) })
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rated Game (Glicko-2)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = rated, onCheckedChange = { rated = it })
                }
                if (rated) {
                    val myR = when (tc.category) {
                        "bullet" -> (myProfile?.bulletRating ?: 1500L).toDouble()
                        "rapid" -> (myProfile?.rapidRating ?: 1500L).toDouble()
                        "classical" -> (myProfile?.classicalRating ?: 1500L).toDouble()
                        else -> (myProfile?.blitzRating ?: 1500L).toDouble()
                    }
                    val previewWin = Glicko2RatingSystem.updateSingleGame(GlickoRating(myR), GlickoRating(myR), 1.0).displayInt - myR.toInt()
                    val previewLoss = Glicko2RatingSystem.updateSingleGame(GlickoRating(myR), GlickoRating(myR), 0.0).displayInt - myR.toInt()
                    Text(
                        text = "Est. vs equal opponent: Win +$previewWin / Loss $previewLoss (Your ${tc.category}: ${myR.toInt()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                if (searchingTicketId != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                Text("Waiting for opponent (${tc.label})...")
                            }
                            TextButton(onClick = onCancelMatchmaking) { Text("Cancel") }
                        }
                    }
                } else {
                    Button(
                        onClick = { onStartMatchmaking(variant, tc, rated) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("find_online_match_btn"),
                    ) {
                        Text("Find Online Match")
                    }
                }
            }
        }
    } else {
        val variant = remember(activeGame.variant) {
            ChessVariant.entries.firstOrNull { it.name == activeGame.variant } ?: ChessVariant.STANDARD
        }
        val boardState = remember(activeGame.fen, variant) {
            ChessBoardState.fromFen(activeGame.fen, variant).getOrElse { ChessBoardState.standard() }
        }
        val legalMoves = remember(boardState) { MoveGenerator.legalMoves(boardState) }
        val amWhite = activeGame.whiteUid == currentUserId
        val myColor = if (amWhite) PieceColor.WHITE else PieceColor.BLACK
        val isSpectator = activeGame.whiteUid != currentUserId && activeGame.blackUid != currentUserId
        var chatDraft by remember { mutableStateOf("") }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PlayerAvatarView(
                            avatarId = settings.localAvatarId,
                            side = if (amWhite) PieceColor.WHITE else PieceColor.BLACK,
                            size = 36.dp,
                            pieceStyleId = settings.pieceStyleId,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${activeGame.whiteName} vs ${activeGame.blackName} (${activeGame.timeControl})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = when (connectionState) {
                                    WebSocketConnectionState.CONNECTED -> "Live WebSocket • Server clocks synced"
                                    WebSocketConnectionState.RECONNECTING,
                                    WebSocketConnectionState.CONNECTING,
                                    -> "Reconnecting to game server..."
                                    WebSocketConnectionState.DISCONNECTED -> "Disconnected • Tap Reconnect"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectionState == WebSocketConnectionState.CONNECTED) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                    Row {
                        if (connectionState != WebSocketConnectionState.CONNECTED) {
                            TextButton(onClick = onReconnectOnline) { Text("Reconnect") }
                        }
                        TextButton(onClick = onCloseGame) { Text("Leave") }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChessBoardView(
                        state = boardState,
                        legalMoves = legalMoves,
                        theme = boardTheme,
                        modifier = Modifier.widthIn(max = 520.dp),
                        flipped = !amWhite && !isSpectator,
                        interactive = activeGame.status == "active" && !isSpectator,
                        allowBothSides = false,
                        humanSide = myColor,
                        showCoordinates = settings.showCoordinates,
                        showLegalMoves = settings.showLegalMoves,
                        showLastMove = settings.showLastMove,
                        autoQueen = settings.autoQueen,
                        pieceStyleId = settings.pieceStyleId,
                        moveAnimation = settings.moveAnimation,
                        reducedAnimations = settings.reducedAnimations,
                        animationMode = settings.animationMode,
                        animationSpeed = settings.animationSpeed,
                        onMoveSelected = onOnlineMove,
                    )
                }
            }
            if (activeGame.status == "active" && !isSpectator) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = onOfferOnlineDraw) {
                            Text(if (activeGame.drawOfferBy.isNotBlank() && activeGame.drawOfferBy != currentUserId) "Accept Draw" else "Offer Draw")
                        }
                        OutlinedButton(onClick = onResignOnline) { Text("Resign") }
                    }
                }
            } else if (activeGame.status == "finished") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${activeGame.result} • ${activeGame.resultReason}", fontWeight = FontWeight.Bold)
                                Button(onClick = { onAnalyzePgn(activeGame.pgn) }) { Text("Analyze") }
                            }
                            if (ratingDelta != null) {
                                val sign = if (ratingDelta.delta >= 0) "+${ratingDelta.delta}" else "${ratingDelta.delta}"
                                Text(
                                    text = if (ratingDelta.ratedGame) {
                                        "Rating: ${ratingDelta.oldRating} → ${ratingDelta.newRating} ($sign)"
                                    } else {
                                        "Casual — Unrated (${ratingDelta.oldRating})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else if (!activeGame.ratedGame) {
                                Text("Casual — Unrated", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            if (settings.allowChatInGames && !isSpectator) {
                item {
                    Text("Game Chat", style = MaterialTheme.typography.labelLarge)
                    chatMessages.takeLast(6).forEach { msg ->
                        Text("${msg.senderName}: ${msg.text}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = { Text("Send message...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        IconButton(onClick = {
                            if (chatDraft.isNotBlank()) {
                                onSendChat(chatDraft)
                                chatDraft = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotSelectorChipCard(
    bot: BotProfile,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 128.dp, max = 168.dp)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color(bot.avatarSpec.accentHex) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BotAvatarView(bot = bot, size = 34.dp, cornerRadius = 9.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = bot.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${bot.approxStrengthLabel} • ${bot.voiceProfile.voiceStyle.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun botForColor(state: LocalGameUiState, color: PieceColor): BotProfile? = when (state.mode) {
    PlayMode.PASS_AND_PLAY -> null
    PlayMode.VS_BOT -> if (color != state.humanColor) state.bot else null
    PlayMode.BOT_VS_BOT -> if (color == PieceColor.WHITE) state.bot else state.botBlack
}

private fun playerLabelFor(state: LocalGameUiState, color: PieceColor): String = when (state.mode) {
    PlayMode.PASS_AND_PLAY -> if (color == PieceColor.WHITE) "Player White" else "Player Black"
    PlayMode.VS_BOT -> if (color == state.humanColor) "You" else state.bot.name
    PlayMode.BOT_VS_BOT -> if (color == PieceColor.WHITE) state.bot.name else state.botBlack.name
}

private fun playerSubtitleFor(state: LocalGameUiState, color: PieceColor): String = when (state.mode) {
    PlayMode.PASS_AND_PLAY -> "${if (color == PieceColor.WHITE) "White" else "Black"} • Over the Board"
    PlayMode.VS_BOT -> if (color == state.humanColor) {
        "${if (color == PieceColor.WHITE) "White" else "Black"} • Human"
    } else {
        "${state.bot.approxStrengthLabel} • ${state.bot.styleTag} • ${state.bot.voiceProfile.voiceStyle.label} Voice"
    }
    PlayMode.BOT_VS_BOT -> {
        val b = if (color == PieceColor.WHITE) state.bot else state.botBlack
        "${b.approxStrengthLabel} • ${b.styleTag} • ${b.voiceProfile.voiceStyle.label} Voice"
    }
}

private fun formatClockMs(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    if (safeMs < 20_000L && safeMs > 0L) {
        val secs = safeMs / 1000L
        val tenths = (safeMs % 1000L) / 100L
        return String.format(java.util.Locale.US, "00:%02d.%d", secs, tenths)
    }
    val totalSec = safeMs / 1000L
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    else String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
