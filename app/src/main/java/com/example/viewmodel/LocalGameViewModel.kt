package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ChessSoundType
import com.example.audio.SoundAndHapticsManager
import com.example.audio.VoiceCoachManager
import com.example.chess.AiDifficulty
import com.example.chess.BotProfile
import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessHubBots
import com.example.chess.ChessVariant
import com.example.chess.ClockType
import com.example.chess.EndgameClassification
import com.example.chess.EndgameClassifier
import com.example.chess.GambitMatch
import com.example.chess.GameReviewReport
import com.example.chess.GameStatus
import com.example.chess.KotlinChessEngineAdapter
import com.example.chess.LocalChessEngine
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveNode
import com.example.chess.MoveQuality
import com.example.chess.MoveValidationResult
import com.example.chess.OpeningBook
import com.example.chess.OpeningMatch
import com.example.chess.PgnParser
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.ReviewedMove
import com.example.chess.TimeControl
import com.example.data.local.AppSettings
import com.example.data.local.LocalChessRepository
import com.example.data.local.LocalGameEntity
import com.example.data.local.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class PlayMode {
    PASS_AND_PLAY,
    VS_BOT,
    BOT_VS_BOT,
}

data class RatingDeltaResult(
    val oldRating: Int,
    val newRating: Int,
    val delta: Int,
    val ratedGame: Boolean,
    val category: String,
)

data class LocalGameUiState(
    val gameId: String = UUID.randomUUID().toString(),
    val mode: PlayMode = PlayMode.VS_BOT,
    val variant: ChessVariant = ChessVariant.STANDARD,
    val timeControl: TimeControl = TimeControl.PRESETS[5],
    val humanColor: PieceColor = PieceColor.WHITE,
    val aiDifficulty: AiDifficulty = AiDifficulty.fromBotProfile(ChessHubBots.ALL[2]),
    val bot: BotProfile = ChessHubBots.ALL[2],
    val botBlack: BotProfile = ChessHubBots.ALL[6],
    val botVsBotPaused: Boolean = false,
    val boardState: ChessBoardState = ChessBoardState.standard(),
    val legalMoves: List<Move> = MoveGenerator.legalMoves(ChessBoardState.standard()),
    val moveHistorySan: List<String> = emptyList(),
    val moveHistoryUci: List<String> = emptyList(),
    val moveHistoryMoves: List<Move> = emptyList(),
    val stateHistory: List<ChessBoardState> = listOf(ChessBoardState.standard()),
    val viewingPly: Int = 0,
    val lastMove: Move? = null,
    val flipped: Boolean = false,
    val whiteTimeMs: Long = 300_000L,
    val blackTimeMs: Long = 300_000L,
    val isClockPaused: Boolean = false,
    val gameStatus: GameStatus = GameStatus.IN_PROGRESS,
    val winner: PieceColor? = null,
    val premoveQueue: List<Move> = emptyList(),
    val selectedDropPiece: PieceType? = null,
    val isBotThinking: Boolean = false,
    val openingMatch: OpeningMatch? = null,
    val gambitMatch: GambitMatch? = null,
    val endgameClassification: EndgameClassification? = null,
    val isCurrentMoveBook: Boolean = false,
    val hintMove: Move? = null,
    val coachMessage: String = "",
    val botDialogueText: String = "",
    val savedToArchive: Boolean = false,
    val pendingConfirmMove: Move? = null,
    val ratedGame: Boolean = true,
    val lastEloDelta: RatingDeltaResult? = null,
    val moveQualities: List<MoveQuality?> = emptyList(),
    val moveExplanations: List<String> = emptyList(),
    val reviewedMoves: List<ReviewedMove> = emptyList(),
    val postGameSummary: GameReviewReport? = null,
    val isGeneratingSummary: Boolean = false,
) {
    val isViewingLive: Boolean
        get() = viewingPly >= moveHistorySan.size

    val displayedBoardState: ChessBoardState
        get() = stateHistory.getOrElse(viewingPly.coerceIn(0, stateHistory.lastIndex.coerceAtLeast(0))) { boardState }

    val displayedLastMove: Move?
        get() = if (viewingPly <= 0) null else moveHistoryMoves.getOrNull(viewingPly - 1)
}

class LocalGameViewModel(
    private val localRepo: LocalChessRepository,
    private val soundManager: SoundAndHapticsManager,
    private val voiceCoach: VoiceCoachManager? = null,
    private val settingsRepo: SettingsRepository? = null,
    private val engineFactory: (lowMemoryMode: Boolean) -> LocalChessEngine = { lowMem ->
        KotlinChessEngineAdapter(lowMemoryMode = lowMem)
    },
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocalGameUiState())
    val uiState: StateFlow<LocalGameUiState> = _uiState.asStateFlow()

    private val positionCounts = mutableMapOf<String, Int>()
    private var rootNode = MoveNode(state = ChessBoardState.standard(), move = null, san = "")
    private var currentNode = rootNode
    private var clockJob: Job? = null
    private var botJob: Job? = null
    private var currentSettings = AppSettings()

    init {
        startNewGame(
            mode = PlayMode.VS_BOT,
            variant = ChessVariant.STANDARD,
            timeControl = TimeControl.PRESETS[5],
            humanColor = PieceColor.WHITE,
            bot = ChessHubBots.ALL[2],
        )
    }

    fun updateSettings(settings: AppSettings) {
        currentSettings = settings
    }

    fun startNewGame(
        mode: PlayMode,
        variant: ChessVariant,
        timeControl: TimeControl,
        humanColor: PieceColor,
        bot: BotProfile = _uiState.value.bot,
        botBlack: BotProfile = _uiState.value.botBlack,
        customInitialFen: String? = null,
        ratedGame: Boolean = _uiState.value.ratedGame,
    ) {
        clockJob?.cancel()
        botJob?.cancel()
        positionCounts.clear()

        val initial = if (!customInitialFen.isNullOrBlank()) {
            ChessBoardState.fromFen(customInitialFen, variant).getOrElse {
                ChessBoardState.initialForVariant(variant)
            }
        } else {
            ChessBoardState.initialForVariant(variant)
        }

        positionCounts[initial.positionKey()] = 1
        rootNode = MoveNode(state = initial, move = null, san = "")
        currentNode = rootNode
        val initialMs = if (timeControl.clockType == ClockType.UNLIMITED) 0L else timeControl.initialSeconds * 1000L
        val startDialogue = if (mode != PlayMode.PASS_AND_PLAY) bot.dialogueFor("start", 0) else ""
        val endgame = if (currentSettings.endgameRecognitionEnabled) EndgameClassifier.classify(initial) else null

        val engine = engineFactory(currentSettings.lowMemoryMode)
        _uiState.value = LocalGameUiState(
            gameId = UUID.randomUUID().toString(),
            mode = mode,
            variant = variant,
            timeControl = timeControl,
            humanColor = humanColor,
            aiDifficulty = AiDifficulty.fromBotProfile(bot),
            bot = bot,
            botBlack = botBlack,
            botVsBotPaused = false,
            boardState = initial,
            legalMoves = engine.legalMoves(initial),
            flipped = mode == PlayMode.VS_BOT && humanColor == PieceColor.BLACK,
            whiteTimeMs = initialMs,
            blackTimeMs = initialMs,
            endgameClassification = endgame,
            botDialogueText = startDialogue,
            ratedGame = ratedGame,
        )

        if (startDialogue.isNotBlank()) {
            voiceCoach?.speakBot(startDialogue, currentSettings, bot = bot)
        }

        startClockLoop()
        if ((mode == PlayMode.VS_BOT && initial.sideToMove != humanColor) || mode == PlayMode.BOT_VS_BOT) {
            triggerBotMove()
        }
    }

    fun toggleRatedGame(rated: Boolean) {
        _uiState.update { it.copy(ratedGame = rated) }
    }

    fun selectDifficulty(difficulty: AiDifficulty) {
        val curr = _uiState.value
        val matchedBot = difficulty.toBotProfile()
        val dialogue = if (curr.mode != PlayMode.PASS_AND_PLAY) matchedBot.dialogueFor("start", curr.moveHistorySan.size) else ""
        _uiState.update {
            it.copy(
                aiDifficulty = difficulty,
                bot = matchedBot,
                botDialogueText = dialogue,
            )
        }
        if (dialogue.isNotBlank()) {
            voiceCoach?.speakBot(dialogue, currentSettings, bot = matchedBot)
        }
    }

    fun speakCurrentBotDialogue() {
        val curr = _uiState.value
        if (curr.mode == PlayMode.PASS_AND_PLAY || curr.botDialogueText.isBlank()) return
        val activeBot = if (curr.mode == PlayMode.BOT_VS_BOT && curr.boardState.sideToMove == PieceColor.BLACK) curr.botBlack else curr.bot
        voiceCoach?.speakBot(curr.botDialogueText, currentSettings, bot = activeBot, isKeyAlert = true)
    }

    fun isMoveValid(move: Move): Boolean =
        engineFactory(currentSettings.lowMemoryMode).isMoveLegal(_uiState.value.boardState, move)

    fun toggleBotVsBotPause() {
        val curr = _uiState.value
        if (curr.mode != PlayMode.BOT_VS_BOT || curr.gameStatus.isTerminal) return
        val nextPaused = !curr.botVsBotPaused
        _uiState.update { it.copy(botVsBotPaused = nextPaused) }
        if (nextPaused) {
            botJob?.cancel()
            _uiState.update { it.copy(isBotThinking = false) }
        } else {
            triggerBotMove()
        }
    }

    fun stepBotVsBotNext() {
        val curr = _uiState.value
        if (curr.mode != PlayMode.BOT_VS_BOT || curr.gameStatus.isTerminal || curr.isBotThinking) return
        triggerBotMove(forceSingleStep = true)
    }

    fun jumpToPly(ply: Int) {
        _uiState.update { curr ->
            val clamped = ply.coerceIn(0, curr.moveHistorySan.size)
            curr.copy(viewingPly = clamped)
        }
    }

    fun stepFirstMove() = jumpToPly(0)

    fun stepPreviousMove() = jumpToPly(_uiState.value.viewingPly - 1)

    fun stepNextMove() = jumpToPly(_uiState.value.viewingPly + 1)

    fun stepLatestMove() = jumpToPly(_uiState.value.moveHistorySan.size)

    fun importPgnIntoLocalGame(pgnText: String): Boolean {
        val parsed = PgnParser.parseMultiGamePgn(pgnText).getOrNull()?.firstOrNull() ?: return false
        clockJob?.cancel()
        botJob?.cancel()
        positionCounts.clear()

        val mainNodes = parsed.rootNode.mainLine()
        val initial = parsed.rootNode.state
        val engine = engineFactory(currentSettings.lowMemoryMode)

        rootNode = parsed.rootNode
        currentNode = if (mainNodes.isEmpty()) parsed.rootNode else mainNodes.last()

        val states = mutableListOf(initial)
        val sans = mutableListOf<String>()
        val ucis = mutableListOf<String>()
        val moves = mutableListOf<Move>()
        positionCounts[initial.positionKey()] = 1

        mainNodes.forEach { node ->
            val m = node.move ?: return@forEach
            states.add(node.state)
            sans.add(node.san)
            ucis.add(m.uci)
            moves.add(m)
            val key = node.state.positionKey()
            positionCounts[key] = (positionCounts[key] ?: 0) + 1
        }

        val finalState = states.last()
        val (status, winner) = MoveGenerator.evaluateGameResult(finalState, positionCounts)
        val tc = _uiState.value.timeControl
        val initialMs = if (tc.clockType == ClockType.UNLIMITED) 0L else tc.initialSeconds * 1000L

        _uiState.value = LocalGameUiState(
            gameId = UUID.randomUUID().toString(),
            mode = PlayMode.PASS_AND_PLAY,
            variant = initial.variant,
            timeControl = tc,
            humanColor = PieceColor.WHITE,
            aiDifficulty = _uiState.value.aiDifficulty,
            bot = _uiState.value.bot,
            botBlack = _uiState.value.botBlack,
            boardState = finalState,
            legalMoves = if (status.isTerminal) emptyList() else engine.legalMoves(finalState),
            moveHistorySan = sans,
            moveHistoryUci = ucis,
            moveHistoryMoves = moves,
            stateHistory = states,
            viewingPly = sans.size,
            lastMove = moves.lastOrNull(),
            whiteTimeMs = initialMs,
            blackTimeMs = initialMs,
            gameStatus = status,
            winner = winner,
            openingMatch = if (initial.variant == ChessVariant.STANDARD) OpeningBook.recognize(ucis) else null,
            gambitMatch = if (initial.variant == ChessVariant.STANDARD) OpeningBook.recognizeGambit(ucis) else null,
            endgameClassification = if (currentSettings.endgameRecognitionEnabled) EndgameClassifier.classify(finalState) else null,
            coachMessage = "Loaded PGN (${sans.size} plies). Use the move list or arrows to step back and forth.",
        )
        startClockLoop()
        return true
    }

    fun onUserMove(move: Move) {
        val curr = _uiState.value
        if (!curr.isViewingLive || curr.gameStatus.isTerminal || curr.mode == PlayMode.BOT_VS_BOT) return
        if (currentSettings.confirmMoves && curr.pendingConfirmMove == null) {
            _uiState.update { it.copy(pendingConfirmMove = move) }
            return
        }
        executeMove(move)
    }

    fun confirmPendingMove() {
        val m = _uiState.value.pendingConfirmMove ?: return
        _uiState.update { it.copy(pendingConfirmMove = null) }
        executeMove(m)
    }

    fun cancelPendingMove() {
        _uiState.update { it.copy(pendingConfirmMove = null) }
    }

    fun queuePremove(move: Move) {
        if (!currentSettings.premoveEnabled) return
        _uiState.update { it.copy(premoveQueue = it.premoveQueue + move) }
    }

    fun clearPremoves() {
        _uiState.update { it.copy(premoveQueue = emptyList()) }
    }

    fun selectDropPiece(type: PieceType?) {
        _uiState.update { it.copy(selectedDropPiece = if (it.selectedDropPiece == type) null else type) }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun toggleClockPause() {
        if (_uiState.value.mode == PlayMode.PASS_AND_PLAY) {
            _uiState.update { it.copy(isClockPaused = !it.isClockPaused) }
        }
    }

    fun resign(resigningColor: PieceColor = _uiState.value.let { if (it.mode == PlayMode.VS_BOT) it.humanColor else it.boardState.sideToMove }) {
        val curr = _uiState.value
        if (curr.gameStatus.isTerminal) return
        finishGame(GameStatus.RESIGNATION, resigningColor.opposite)
    }

    fun agreeDraw() {
        val curr = _uiState.value
        if (curr.gameStatus.isTerminal) return
        finishGame(GameStatus.DRAW_BY_AGREEMENT, null)
    }

    fun takebackMove() {
        val curr = _uiState.value
        if (curr.gameStatus.isTerminal || curr.stateHistory.size <= 1) return
        botJob?.cancel()
        val stepsBack = if (curr.mode == PlayMode.VS_BOT && curr.stateHistory.size >= 3 && curr.boardState.sideToMove == curr.humanColor) 2 else 1
        val newStates = curr.stateHistory.dropLast(stepsBack)
        val newSans = curr.moveHistorySan.dropLast(stepsBack)
        val newUcis = curr.moveHistoryUci.dropLast(stepsBack)
        val newMoves = curr.moveHistoryMoves.dropLast(stepsBack)
        val newQualities = curr.moveQualities.dropLast(stepsBack.coerceAtMost(curr.moveQualities.size))
        val newExplanations = curr.moveExplanations.dropLast(stepsBack.coerceAtMost(curr.moveExplanations.size))
        val newReviewed = curr.reviewedMoves.filter { it.ply <= newSans.size }
        val restored = newStates.last()

        repeat(stepsBack) {
            currentNode.parent?.let { parent ->
                parent.children.remove(currentNode)
                currentNode = parent
            }
        }
        positionCounts.clear()
        newStates.forEach { st ->
            val k = st.positionKey()
            positionCounts[k] = (positionCounts[k] ?: 0) + 1
        }
        val engine = engineFactory(currentSettings.lowMemoryMode)
        _uiState.update {
            it.copy(
                boardState = restored,
                legalMoves = engine.legalMoves(restored),
                stateHistory = newStates,
                moveHistorySan = newSans,
                moveHistoryUci = newUcis,
                moveHistoryMoves = newMoves,
                moveQualities = newQualities,
                moveExplanations = newExplanations,
                reviewedMoves = newReviewed,
                viewingPly = newSans.size,
                lastMove = currentNode.move,
                isBotThinking = false,
                hintMove = null,
                premoveQueue = emptyList(),
                endgameClassification = if (currentSettings.endgameRecognitionEnabled) EndgameClassifier.classify(restored) else null,
            )
        }
    }

    fun requestHint() {
        val curr = _uiState.value
        if (curr.gameStatus.isTerminal || curr.isBotThinking) return
        viewModelScope.launch {
            val engine = engineFactory(currentSettings.lowMemoryMode)
            val hintDifficulty = when {
                currentSettings.engineDepth >= 4 -> AiDifficulty.EXPERT
                currentSettings.engineDepth == 3 -> AiDifficulty.HARD
                else -> AiDifficulty.MEDIUM
            }
            val line = engine.evaluateBestMove(curr.boardState, difficulty = hintDifficulty)
            val best = line?.pvMoves?.firstOrNull()
            val coach = engine.explainPosition(curr.boardState, line)
            _uiState.update { it.copy(hintMove = best, coachMessage = coach) }
            voiceCoach?.speakTrainer(coach, currentSettings)
        }
    }

    private fun executeMove(move: Move, fromBot: Boolean = false) {
        val curr = _uiState.value
        if (curr.gameStatus.isTerminal) return
        val engine = engineFactory(currentSettings.lowMemoryMode)
        val validation = engine.validateMove(curr.boardState, move, positionCounts)
        if (validation !is MoveValidationResult.Valid) {
            soundManager.play(ChessSoundType.ERROR, currentSettings)
            return
        }

        val validMove = validation.resolvedMove
        val san = validation.san
        val nextState = validation.nextState
        val status = validation.gameStatus
        val winner = validation.winner
        val isCheckNow = validation.isCheck

        val mover = curr.boardState.sideToMove
        val wasCapture = curr.boardState.pieceAt(validMove.to) != null || validMove.isEnPassant
        val posKey = nextState.positionKey()
        positionCounts[posKey] = (positionCounts[posKey] ?: 0) + 1
        currentNode = currentNode.addOrGetChild(validMove, san, nextState)

        val stateBefore = curr.boardState
        val historyBefore = curr.moveHistoryUci
        val moveIdx = curr.moveHistorySan.size
        val gameIdSnapshot = curr.gameId

        val newUcis = curr.moveHistoryUci + validMove.uci
        val openingMatch = if (curr.variant == ChessVariant.STANDARD && currentSettings.openingRecognitionEnabled) {
            OpeningBook.recognize(newUcis)
        } else null
        val gambitMatch = if (curr.variant == ChessVariant.STANDARD && currentSettings.gambitRecognitionEnabled) {
            OpeningBook.recognizeGambit(newUcis)
        } else null
        val endgame = if (currentSettings.endgameRecognitionEnabled) EndgameClassifier.classify(nextState) else null
        val isBook = curr.variant == ChessVariant.STANDARD &&
            currentSettings.openingBookEnabled &&
            historyBefore.size < 16 &&
            OpeningBook.isBookMove(historyBefore, validMove.uci)

        val (newWhiteMs, newBlackMs) = applyIncrement(curr.whiteTimeMs, curr.blackTimeMs, mover, curr.timeControl)

        val soundType = when {
            status.isTerminal -> ChessSoundType.GAME_END
            isCheckNow -> ChessSoundType.CHECK
            validMove.promotion != null -> ChessSoundType.PROMOTION
            validMove.isCastling -> ChessSoundType.CASTLE
            wasCapture -> ChessSoundType.CAPTURE
            isBook && currentSettings.sfxBookMove -> ChessSoundType.BOOK_MOVE
            else -> ChessSoundType.MOVE
        }
        soundManager.play(soundType, currentSettings)

        var dialogue = curr.botDialogueText
        if (fromBot || status.isTerminal) {
            val activeBot = if (curr.mode == PlayMode.BOT_VS_BOT && mover == PieceColor.BLACK) curr.botBlack else curr.bot
            val capturedPiece = if (validMove.isEnPassant) PieceType.PAWN else curr.boardState.pieceAt(validMove.to)?.type
            val newOpeningDiscovered = openingMatch != null && curr.openingMatch == null
            val enteredEndgame = endgame != null && curr.endgameClassification == null
            val event = when {
                status.isTerminal && winner == mover -> "win"
                status.isTerminal && winner != null -> "lose"
                status.isTerminal -> "draw"
                isCheckNow -> "check"
                validMove.promotion != null -> "promotion"
                validMove.isCastling -> "castled"
                wasCapture -> "capture"
                enteredEndgame -> "endgame"
                newOpeningDiscovered -> "opening"
                else -> "move"
            }
            if (curr.mode != PlayMode.PASS_AND_PLAY &&
                (status.isTerminal || isCheckNow || wasCapture || validMove.promotion != null || validMove.isCastling || enteredEndgame || newOpeningDiscovered || newUcis.size % 5 == 0)
            ) {
                dialogue = activeBot.dialogueFor(
                    event = event,
                    moveCount = newUcis.size,
                    san = san,
                    openingName = openingMatch?.name,
                    capturedPiece = capturedPiece,
                )
                voiceCoach?.speakBot(dialogue, currentSettings, bot = activeBot, isKeyAlert = status.isTerminal || isCheckNow)
            }
        }

        _uiState.update {
            val updatedSans = it.moveHistorySan + san
            it.copy(
                boardState = nextState,
                legalMoves = if (status.isTerminal) emptyList() else engine.legalMoves(nextState),
                moveHistorySan = updatedSans,
                moveHistoryUci = newUcis,
                moveHistoryMoves = it.moveHistoryMoves + validMove,
                stateHistory = it.stateHistory + nextState,
                moveQualities = it.moveQualities + if (isBook) MoveQuality.BOOK else null,
                moveExplanations = it.moveExplanations + if (isBook) "$san is an established opening book move." else "",
                viewingPly = updatedSans.size,
                lastMove = validMove,
                whiteTimeMs = newWhiteMs,
                blackTimeMs = newBlackMs,
                gameStatus = status,
                winner = winner,
                selectedDropPiece = null,
                hintMove = null,
                openingMatch = openingMatch ?: it.openingMatch,
                gambitMatch = gambitMatch ?: it.gambitMatch,
                endgameClassification = endgame,
                isCurrentMoveBook = isBook,
                botDialogueText = dialogue,
                pendingConfirmMove = null,
            )
        }

        if (currentSettings.moveFeedbackEnabled && !fromBot && !status.isTerminal) {
            viewModelScope.launch {
                val reviewed = ChessEngine(currentSettings.lowMemoryMode).evaluateSingleMove(
                    stateBefore = stateBefore,
                    move = validMove,
                    historyUciBefore = historyBefore,
                    useOpeningBook = currentSettings.openingBookEnabled,
                )
                soundManager.playQualityFeedback(reviewed.quality, currentSettings)
                var botReactionText: String? = null
                if (curr.mode == PlayMode.VS_BOT && (reviewed.quality == MoveQuality.BLUNDER || reviewed.quality == MoveQuality.BRILLIANT || reviewed.quality == MoveQuality.GREAT)) {
                    val reactEvent = if (reviewed.quality == MoveQuality.BLUNDER) "blunder_opp" else "great_opp"
                    botReactionText = curr.bot.dialogueFor(reactEvent, newUcis.size, san = reviewed.san)
                    voiceCoach?.speakBot(botReactionText, currentSettings, bot = curr.bot)
                }
                _uiState.update { latest ->
                    if (latest.gameId != gameIdSnapshot || moveIdx >= latest.moveQualities.size) {
                        latest
                    } else {
                        val nextQualities = latest.moveQualities.toMutableList().apply { set(moveIdx, reviewed.quality) }
                        val nextExplanations = latest.moveExplanations.toMutableList().apply { set(moveIdx, reviewed.coachExplanation) }
                        val nextReviewed = (latest.reviewedMoves.filterNot { it.ply == reviewed.ply } + reviewed).sortedBy { it.ply }
                        latest.copy(
                            moveQualities = nextQualities,
                            moveExplanations = nextExplanations,
                            reviewedMoves = nextReviewed,
                            botDialogueText = botReactionText ?: latest.botDialogueText,
                            coachMessage = "${reviewed.quality.symbol} ${reviewed.quality.label}: ${reviewed.coachExplanation}",
                        )
                    }
                }
            }
        }

        if (status.isTerminal) {
            saveFinishedGameToArchive()
            return
        }

        if (curr.mode == PlayMode.BOT_VS_BOT && !curr.botVsBotPaused) {
            triggerBotMove()
        } else if (curr.mode == PlayMode.VS_BOT && nextState.sideToMove != curr.humanColor) {
            triggerBotMove()
        } else if (curr.mode == PlayMode.VS_BOT && nextState.sideToMove == curr.humanColor) {
            val premove = _uiState.value.premoveQueue.firstOrNull()
            if (premove != null) {
                _uiState.update { it.copy(premoveQueue = it.premoveQueue.drop(1)) }
                val legalNow = _uiState.value.legalMoves
                val matched = legalNow.firstOrNull { it.from == premove.from && it.to == premove.to }
                if (matched != null) {
                    executeMove(matched)
                } else {
                    _uiState.update { it.copy(premoveQueue = emptyList()) }
                }
            }
        }
    }

    private fun triggerBotMove(forceSingleStep: Boolean = false) {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            val curr = _uiState.value
            if (curr.mode == PlayMode.BOT_VS_BOT && curr.botVsBotPaused && !forceSingleStep) return@launch
            val activeBot = if (curr.mode == PlayMode.BOT_VS_BOT && curr.boardState.sideToMove == PieceColor.BLACK) {
                curr.botBlack
            } else {
                curr.bot
            }
            _uiState.update { it.copy(isBotThinking = true) }
            val engine = engineFactory(currentSettings.lowMemoryMode)
            val chosen = engine.chooseBotMove(
                state = curr.boardState,
                bot = activeBot,
                playedUciMoves = curr.moveHistoryUci,
                useOpeningBook = currentSettings.openingBookEnabled,
            )
            delay(if (currentSettings.lowMemoryMode) 150L else if (curr.mode == PlayMode.BOT_VS_BOT) 380L else 260L)
            _uiState.update { it.copy(isBotThinking = false) }
            if (chosen != null) {
                executeMove(chosen, fromBot = true)
            }
        }
    }

    private fun applyIncrement(
        whiteMs: Long,
        blackMs: Long,
        mover: PieceColor,
        tc: TimeControl,
    ): Pair<Long, Long> {
        if (tc.clockType == ClockType.UNLIMITED) return whiteMs to blackMs
        val incMs = tc.incrementSeconds * 1000L
        return if (mover == PieceColor.WHITE) (whiteMs + incMs) to blackMs
        else whiteMs to (blackMs + incMs)
    }

    private fun startClockLoop() {
        clockJob?.cancel()
        if (_uiState.value.timeControl.clockType == ClockType.UNLIMITED) return
        clockJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (true) {
                delay(200L)
                val now = System.currentTimeMillis()
                val dt = now - lastTick
                lastTick = now
                val curr = _uiState.value
                if (curr.gameStatus.isTerminal || curr.isClockPaused || curr.moveHistorySan.isEmpty()) continue
                if (curr.boardState.sideToMove == PieceColor.WHITE) {
                    val rem = (curr.whiteTimeMs - dt).coerceAtLeast(0L)
                    _uiState.update { it.copy(whiteTimeMs = rem) }
                    if (rem == 0L) {
                        finishGame(GameStatus.TIMEOUT, PieceColor.BLACK)
                        break
                    }
                } else {
                    val rem = (curr.blackTimeMs - dt).coerceAtLeast(0L)
                    _uiState.update { it.copy(blackTimeMs = rem) }
                    if (rem == 0L) {
                        finishGame(GameStatus.TIMEOUT, PieceColor.WHITE)
                        break
                    }
                }
            }
        }
    }

    private fun finishGame(status: GameStatus, winner: PieceColor?) {
        clockJob?.cancel()
        botJob?.cancel()
        soundManager.play(ChessSoundType.GAME_END, currentSettings)
        _uiState.update {
            it.copy(
                gameStatus = status,
                winner = winner,
                legalMoves = emptyList(),
                isBotThinking = false,
                premoveQueue = emptyList(),
            )
        }
        saveFinishedGameToArchive()
    }

    fun exportCurrentPgn(): String {
        val curr = _uiState.value
        val resultStr = when {
            !curr.gameStatus.isTerminal -> "*"
            curr.winner == PieceColor.WHITE -> "1-0"
            curr.winner == PieceColor.BLACK -> "0-1"
            else -> "1/2-1/2"
        }
        val whiteName = when (curr.mode) {
            PlayMode.VS_BOT -> if (curr.humanColor == PieceColor.BLACK) "${curr.bot.name} (${curr.bot.approxStrengthLabel})" else "Player White"
            PlayMode.BOT_VS_BOT -> "${curr.bot.name} (${curr.bot.approxStrengthLabel})"
            PlayMode.PASS_AND_PLAY -> "Player White"
        }
        val blackName = when (curr.mode) {
            PlayMode.VS_BOT -> if (curr.humanColor == PieceColor.WHITE) "${curr.bot.name} (${curr.bot.approxStrengthLabel})" else "Player Black"
            PlayMode.BOT_VS_BOT -> "${curr.botBlack.name} (${curr.botBlack.approxStrengthLabel})"
            PlayMode.PASS_AND_PLAY -> "Player Black"
        }
        val headers = mapOf(
            "Event" to when (curr.mode) {
                PlayMode.VS_BOT -> "Gamebit vs ${curr.bot.name}"
                PlayMode.BOT_VS_BOT -> "${curr.bot.name} vs ${curr.botBlack.name}"
                PlayMode.PASS_AND_PLAY -> "Gamebit Over The Board"
            },
            "White" to whiteName,
            "Black" to blackName,
            "Variant" to curr.variant.displayName,
            "TimeControl" to curr.timeControl.label,
            "ECO" to (curr.openingMatch?.eco ?: ""),
            "Opening" to (curr.openingMatch?.name ?: ""),
            "FEN" to rootNode.state.toFen(),
        ).filterValues { it.isNotBlank() }
        return PgnParser.exportPgn(rootNode, headers, resultStr)
    }

    fun saveGameSnapshotManually() {
        saveFinishedGameToArchive(forceSave = true)
    }

    private fun saveFinishedGameToArchive(forceSave: Boolean = false) {
        val curr = _uiState.value
        if (curr.moveHistorySan.isEmpty()) return
        if (curr.savedToArchive && !forceSave) return
        val resultStr = when {
            !curr.gameStatus.isTerminal -> "*"
            curr.winner == PieceColor.WHITE -> "1-0"
            curr.winner == PieceColor.BLACK -> "0-1"
            else -> "1/2-1/2"
        }
        val gameIdSnapshot = curr.gameId
        val entity = LocalGameEntity(
            id = curr.gameId,
            whiteName = when (curr.mode) {
                PlayMode.VS_BOT -> if (curr.humanColor == PieceColor.BLACK) curr.bot.name else "You"
                PlayMode.BOT_VS_BOT -> curr.bot.name
                PlayMode.PASS_AND_PLAY -> "White"
            },
            blackName = when (curr.mode) {
                PlayMode.VS_BOT -> if (curr.humanColor == PieceColor.WHITE) curr.bot.name else "Opponent"
                PlayMode.BOT_VS_BOT -> curr.botBlack.name
                PlayMode.PASS_AND_PLAY -> "Black"
            },
            whiteRating = if (curr.mode == PlayMode.BOT_VS_BOT || (curr.mode == PlayMode.VS_BOT && curr.humanColor == PieceColor.BLACK)) curr.bot.approxElo else 1500,
            blackRating = when (curr.mode) {
                PlayMode.BOT_VS_BOT -> curr.botBlack.approxElo
                PlayMode.VS_BOT -> if (curr.humanColor == PieceColor.WHITE) curr.bot.approxElo else 1500
                PlayMode.PASS_AND_PLAY -> 1500
            },
            result = resultStr,
            resultReason = if (curr.gameStatus.isTerminal) curr.gameStatus.description else "In Progress",
            variant = curr.variant.displayName,
            timeControl = curr.timeControl.label,
            mode = curr.mode.name.lowercase(),
            openingEco = curr.openingMatch?.eco ?: "",
            openingName = curr.openingMatch?.name ?: "",
            initialFen = rootNode.state.toFen(),
            finalFen = curr.boardState.toFen(),
            pgn = exportCurrentPgn(),
        )
        _uiState.update { it.copy(isGeneratingSummary = curr.gameStatus.isTerminal && curr.postGameSummary == null) }
        viewModelScope.launch {
            var deltaResult = curr.lastEloDelta
            if (!forceSave && curr.gameStatus.isTerminal && curr.mode == PlayMode.VS_BOT &&
                curr.ratedGame && curr.moveHistorySan.size >= 4 && settingsRepo != null && deltaResult == null
            ) {
                val score = when (curr.winner) {
                    curr.humanColor -> 1.0
                    null -> 0.5
                    else -> 0.0
                }
                val stats = settingsRepo.recordLocalGameRating(
                    category = curr.timeControl.category,
                    opponentRating = curr.bot.approxElo,
                    score = score,
                )
                deltaResult = RatingDeltaResult(
                    oldRating = stats.previousRating,
                    newRating = stats.rating,
                    delta = stats.lastDelta,
                    ratedGame = true,
                    category = curr.timeControl.category,
                )
            }

            val report = curr.postGameSummary ?: if (curr.gameStatus.isTerminal) {
                ChessEngine(currentSettings.lowMemoryMode).reviewGame(
                    initialState = rootNode.state,
                    moves = curr.moveHistoryMoves,
                    useOpeningBook = currentSettings.openingBookEnabled,
                )
            } else null

            localRepo.saveGame(
                entity.copy(
                    whiteAccuracy = report?.whiteAccuracy,
                    blackAccuracy = report?.blackAccuracy,
                ),
            )
            _uiState.update { latest ->
                if (latest.gameId != gameIdSnapshot) latest
                else latest.copy(
                    savedToArchive = true,
                    lastEloDelta = deltaResult ?: latest.lastEloDelta,
                    postGameSummary = report ?: latest.postGameSummary,
                    reviewedMoves = report?.moves ?: latest.reviewedMoves,
                    moveQualities = report?.moves?.map { it.quality } ?: latest.moveQualities,
                    moveExplanations = report?.moves?.map { it.coachExplanation } ?: latest.moveExplanations,
                    isGeneratingSummary = false,
                    coachMessage = if (forceSave) "Game saved to local archive." else latest.coachMessage,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        botJob?.cancel()
    }
}

