package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.BoardArrow
import com.example.chess.ChessBoardState
import com.example.chess.ChessHubBots
import com.example.chess.ChessVariant
import com.example.chess.ClockType
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.TimeControl
import com.example.data.local.AppSettings
import com.example.data.remote.GameChatMessageDoc
import com.example.data.remote.OnlineGameDoc
import com.example.ui.components.ChessBoardView
import com.example.ui.components.PieceMiniIcon
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
    searchingTicketId: String?,
    activeOnlineGame: OnlineGameDoc?,
    onlineChatMessages: List<GameChatMessageDoc>,
    onLocalMove: (Move) -> Unit,
    onQueuePremove: (Move) -> Unit,
    onClearPremoves: () -> Unit,
    onConfirmMove: () -> Unit,
    onCancelConfirmMove: () -> Unit,
    onSelectDropPiece: (PieceType?) -> Unit,
    onStartNewLocalGame: (PlayMode, ChessVariant, TimeControl, PieceColor, com.example.chess.BotProfile) -> Unit,
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
                text = { Text(if (localState.mode == PlayMode.VS_BOT) "Play vs Bot" else "Pass & Play (OTB)") },
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
                    )
                },
                onAnalyzePgn = onAnalyzePgn,
                onShareText = onShareText,
            )
        } else {
            OnlinePlayContent(
                isOnline = isOnline,
                currentUserId = currentUserId,
                settings = settings,
                boardTheme = boardTheme,
                searchingTicketId = searchingTicketId,
                activeGame = activeOnlineGame,
                chatMessages = onlineChatMessages,
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
            onStart = { mode, variant, tc, color, bot ->
                showSetupSheet = false
                onStartNewLocalGame(mode, variant, tc, color, bot)
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
    onToggleFlip: () -> Unit,
    onToggleClockPause: () -> Unit,
    onTakeback: () -> Unit,
    onRequestHint: () -> Unit,
    onResign: () -> Unit,
    onAgreeDraw: () -> Unit,
    onRematch: () -> Unit,
    onAnalyzePgn: (String) -> Unit,
    onShareText: (String, String) -> Unit,
) {
    var dismissResultBanner by remember(state.gameId) { mutableStateOf(false) }

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
                    Text(
                        text = "${state.variant.displayName} • ${state.timeControl.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val openText = state.openingMatch?.let { "${it.eco}: ${it.name}" } ?: "Ready"
                    Text(
                        text = if (state.isCurrentMoveBook) "Book: $openText" else openText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                timeMs = if (topColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs,
                isTurn = !state.gameStatus.isTerminal && state.boardState.sideToMove == topColor,
                isUnlimited = state.timeControl.clockType == ClockType.UNLIMITED,
                isThinking = state.isBotThinking && state.mode == PlayMode.VS_BOT && topColor != state.humanColor,
                checksGiven = if (state.variant == ChessVariant.THREE_CHECK) {
                    if (topColor == PieceColor.WHITE) (3 - state.boardState.blackChecksRemaining) else (3 - state.boardState.whiteChecksRemaining)
                } else null,
            )
        }

        // Chessboard (never obscured during active play!)
        item {
            val arrows = remember(state.hintMove) {
                state.hintMove?.let { listOf(BoardArrow(it.from, it.to, 0xCC10B981)) } ?: emptyList()
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ChessBoardView(
                    state = state.boardState,
                    legalMoves = state.legalMoves,
                    theme = boardTheme,
                    modifier = Modifier.widthIn(max = 520.dp),
                    flipped = state.flipped,
                    interactive = !state.gameStatus.isTerminal,
                    allowBothSides = state.mode == PlayMode.PASS_AND_PLAY,
                    humanSide = state.humanColor,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    lastMove = state.lastMove,
                    premoveQueue = state.premoveQueue,
                    arrows = arrows,
                    selectedDropPiece = state.selectedDropPiece,
                    onMoveSelected = onLocalMove,
                    onPremoveQueued = onQueuePremove,
                )
            }
        }

        // Crazyhouse pocket bar when playing Crazyhouse
        if (state.variant == ChessVariant.CRAZYHOUSE) {
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
                timeMs = if (bottomColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs,
                isTurn = !state.gameStatus.isTerminal && state.boardState.sideToMove == bottomColor,
                isUnlimited = state.timeControl.clockType == ClockType.UNLIMITED,
                isThinking = state.isBotThinking && state.mode == PlayMode.VS_BOT && bottomColor != state.humanColor,
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

        // Action Controls Row
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
                    enabled = !state.gameStatus.isTerminal && state.moveHistorySan.isNotEmpty(),
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
            }
        }

        // Terminal Result Summary Card (appears cleanly below board, or dialog)
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRematch, modifier = Modifier.testTag("rematch_btn")) {
                                Text("Rematch")
                            }
                            FilledTonalButton(
                                onClick = {
                                    // Generate PGN cleanly
                                    val pgn = buildString {
                                        append("[Event \"ChessHub Game\"]\n")
                                        append("[White \"${playerLabelFor(state, PieceColor.WHITE)}\"]\n")
                                        append("[Black \"${playerLabelFor(state, PieceColor.BLACK)}\"]\n\n")
                                        state.moveHistorySan.chunked(2).forEachIndexed { idx, pair ->
                                            append("${idx + 1}. ${pair[0]} ")
                                            if (pair.size > 1) append("${pair[1]} ")
                                        }
                                    }
                                    onAnalyzePgn(pgn)
                                },
                                modifier = Modifier.testTag("review_game_btn"),
                            ) {
                                Text("Review & Analyze")
                            }
                            OutlinedButton(
                                onClick = { onShareText("ChessHub FEN", state.boardState.toFen()) },
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

        // Move list strip
        item {
            if (state.moveHistorySan.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.moveHistorySan.chunked(2).withIndex().toList()) { (idx, pair) ->
                            val text = if (pair.size == 2) "${idx + 1}. ${pair[0]} ${pair[1]}" else "${idx + 1}. ${pair[0]}"
                            Text(text = text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (state.gameStatus.isTerminal && !dismissResultBanner) {
        val headline = when (state.winner) {
            PieceColor.WHITE -> "White Wins!"
            PieceColor.BLACK -> "Black Wins!"
            null -> "Game Drawn"
        }
        AlertDialog(
            onDismissRequest = { dismissResultBanner = true },
            title = { Text(headline) },
            text = { Text("${state.gameStatus.description} after ${state.moveHistorySan.size} plies.") },
            confirmButton = {
                Button(onClick = {
                    dismissResultBanner = true
                    onRematch()
                }) { Text("Rematch") }
            },
            dismissButton = {
                TextButton(onClick = { dismissResultBanner = true }) { Text("View Board") }
            },
        )
    }
}

@Composable
private fun PlayerClockBar(
    name: String,
    timeMs: Long,
    isTurn: Boolean,
    isUnlimited: Boolean,
    isThinking: Boolean,
    checksGiven: Int?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isTurn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (checksGiven != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = "Checks: $checksGiven/3",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (isThinking) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
            val clockText = if (isUnlimited) "∞" else formatClockMs(timeMs)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = clockText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isTurn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
    onStart: (PlayMode, ChessVariant, TimeControl, PieceColor, com.example.chess.BotProfile) -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("New Game Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
            if (mode == PlayMode.VS_BOT) {
                item {
                    Text("Select Bot Opponent", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ChessHubBots.ALL) { b ->
                            FilterChip(
                                selected = bot.id == b.id,
                                onClick = { bot = b },
                                label = { Text("${b.name} (${b.approxStrengthLabel})") },
                            )
                        }
                    }
                    Text(bot.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TimeControl.PRESETS.forEach { tc ->
                            FilterChip(
                                selected = selectedPreset.label == tc.label,
                                onClick = { selectedPreset = tc },
                                label = { Text(tc.label) },
                            )
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
                    onClick = { onStart(mode, variant, chosenTc, humanColor, bot) },
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
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    searchingTicketId: String?,
    activeGame: OnlineGameDoc?,
    chatMessages: List<GameChatMessageDoc>,
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
                Text("Sign in for Real-Time Online Chess", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Matchmaking, Glicko-2 ratings, friend challenges, and live game chat sync securely via Cloud Firestore when signed in with Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestSignIn) { Text("Sign in with Google") }
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
                Text("Online Matchmaking Pool", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Real-time opponent pairing via ChessHub Cloud Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    Text(
                        "${activeGame.whiteName} vs ${activeGame.blackName} (${activeGame.timeControl})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onCloseGame) { Text("Leave") }
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${activeGame.result} • ${activeGame.resultReason}", fontWeight = FontWeight.Bold)
                            Button(onClick = { onAnalyzePgn(activeGame.pgn) }) { Text("Analyze") }
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

private fun playerLabelFor(state: LocalGameUiState, color: PieceColor): String = when (state.mode) {
    PlayMode.PASS_AND_PLAY -> if (color == PieceColor.WHITE) "White (OTB)" else "Black (OTB)"
    PlayMode.VS_BOT -> if (color == state.humanColor) "You" else "${state.bot.name} (${state.bot.approxStrengthLabel})"
}

private fun formatClockMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) String.format("%d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
