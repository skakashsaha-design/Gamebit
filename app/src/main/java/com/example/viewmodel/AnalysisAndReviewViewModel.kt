package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.BoardArrow
import com.example.chess.BookMoveStat
import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessVariant
import com.example.chess.EndgameClassification
import com.example.chess.EndgameClassifier
import com.example.chess.EngineLine
import com.example.chess.GambitMatch
import com.example.chess.GameReviewReport
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveNode
import com.example.chess.OpeningBook
import com.example.chess.OpeningMatch
import com.example.chess.PgnParser
import com.example.chess.Square
import com.example.chess.UciChessEngine
import com.example.data.local.AppSettings
import com.example.data.local.ChessPuzzleEntity
import com.example.data.local.CustomPuzzleEntity
import com.example.data.local.LocalChessRepository
import com.example.data.local.LocalGameEntity
import com.example.data.local.LocalStudyEntity
import com.example.data.local.StudyChapter
import com.example.data.remote.ExternalChessApis
import com.example.data.remote.ExternalExplorerResult
import com.example.data.remote.ExternalTablebaseResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AnalysisUiState(
    val rootNode: MoveNode = MoveNode(state = ChessBoardState.standard(), move = null, san = ""),
    val currentNode: MoveNode = rootNode,
    val legalMoves: List<Move> = MoveGenerator.legalMoves(ChessBoardState.standard()),
    val flipped: Boolean = false,
    val engineEnabled: Boolean = false,
    val engineLines: List<EngineLine> = emptyList(),
    val threatMove: Move? = null,
    val localBookMoves: List<BookMoveStat> = emptyList(),
    val externalExplorer: ExternalExplorerResult? = null,
    val externalExplorerLoading: Boolean = false,
    val tablebaseResult: ExternalTablebaseResult? = null,
    val openingMatch: OpeningMatch? = null,
    val gambitMatch: GambitMatch? = null,
    val endgameClassification: EndgameClassification? = null,
    val coachInsight: String = "",
    val isAutoPlaying: Boolean = false,
    val autoplaySpeedMs: Long = 900L,
    val autoplayMultiplier: Float = 1.0f,
    val reviewReport: GameReviewReport? = null,
    val isReviewing: Boolean = false,
    val statusBanner: String = "",
    val errorText: String? = null,
    val arrows: List<BoardArrow> = emptyList(),
    val userArrows: List<BoardArrow> = emptyList(),
    val userCircleSquares: Set<Square> = emptySet(),
    val uciOutput: List<String> = emptyList(),
    val uciRunning: Boolean = true,
)

class AnalysisAndReviewViewModel(
    private val localRepo: LocalChessRepository,
    private val externalApis: ExternalChessApis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val uciEngine = UciChessEngine()
    private var engineJob: Job? = null
    private var autoplayJob: Job? = null
    private var currentSettings = AppSettings()

    init {
        refreshDerivedForNode(_uiState.value.rootNode)
        viewModelScope.launch {
            uciEngine.outputLog.collect { log ->
                _uiState.update { it.copy(uciOutput = log) }
            }
        }
        viewModelScope.launch {
            uciEngine.isRunning.collect { running ->
                _uiState.update { it.copy(uciRunning = running) }
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        currentSettings = settings
    }

    fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
        autoplayJob?.cancel()
        autoplayJob = null
        _uiState.update { it.copy(engineEnabled = false, isAutoPlaying = false, arrows = emptyList()) }
    }

    fun toggleEngine() {
        val next = !_uiState.value.engineEnabled
        _uiState.update { it.copy(engineEnabled = next) }
        if (next) startEngineForCurrentNode() else stopEngine()
    }

    fun runUciCommand(command: String) {
        viewModelScope.launch {
            uciEngine.sendCommand(command)
        }
    }

    fun runUciBenchmarkHere() {
        viewModelScope.launch {
            uciEngine.sendCommand("uci")
            uciEngine.sendCommand("setoption name MultiPV value ${currentSettings.engineMultiPv}")
            uciEngine.sendCommand("position fen ${_uiState.value.currentNode.state.toFen()}")
            uciEngine.sendCommand("go depth ${currentSettings.engineDepth.coerceIn(1, 4)}")
        }
    }

    fun restartUciEngine() {
        uciEngine.restartEngine()
        _uiState.update { it.copy(statusBanner = "UCI engine restarted") }
    }

    fun toggleUserCircle(square: Square) {
        if (!currentSettings.allowBoardArrows) return
        _uiState.update { st ->
            val next = if (square in st.userCircleSquares) st.userCircleSquares - square else st.userCircleSquares + square
            st.copy(userCircleSquares = next)
        }
    }

    fun toggleUserArrow(from: Square, to: Square) {
        if (!currentSettings.allowBoardArrows || from == to) return
        _uiState.update { st ->
            val exists = st.userArrows.any { it.from == from && it.to == to }
            val next = if (exists) {
                st.userArrows.filterNot { it.from == from && it.to == to }
            } else {
                st.userArrows + BoardArrow(from, to, 0xCC3B82F6)
            }
            st.copy(userArrows = next)
        }
    }

    fun clearUserAnnotations() {
        _uiState.update { it.copy(userArrows = emptyList(), userCircleSquares = emptySet()) }
    }

    fun saveCurrentPositionAsPuzzle(
        title: String,
        solutionMoveUci: String,
        theme: String,
        rating: Int,
        hint: String = "Find the best tactical move from this position.",
    ) {
        val state = _uiState.value.currentNode.state
        val fen = state.toFen()
        val sol = solutionMoveUci.trim().ifBlank {
            _uiState.value.engineLines.firstOrNull()?.pvMoves?.firstOrNull()?.uci ?: ""
        }
        if (sol.isBlank()) {
            _uiState.update { it.copy(errorText = "Provide a solution move or run Engine to auto-fill Best Move.") }
            return
        }
        viewModelScope.launch {
            val puzzle = ChessPuzzleEntity(
                id = "custom_${UUID.randomUUID().toString().take(12)}",
                title = title.trim().ifBlank { "Custom Tactic" },
                fen = fen,
                solutionMoves = sol,
                difficultyRating = rating.coerceIn(400, 3000),
                themes = theme.ifBlank { "Custom" },
                hint = hint,
                explanation = "Created from Analysis / Board Editor ($sol).",
                source = "custom",
            )
            localRepo.savePuzzle(puzzle)
            _uiState.update { it.copy(statusBanner = "Saved '${puzzle.title}' to Room Puzzle Collection!") }
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun playMove(move: Move) {
        val node = _uiState.value.currentNode
        val legal = MoveGenerator.legalMoves(node.state)
        val valid = legal.firstOrNull {
            it.from == move.from && it.to == move.to && it.promotion == move.promotion && it.dropPieceType == move.dropPieceType
        } ?: return
        val san = MoveGenerator.toSan(node.state, valid, legal)
        val nextState = MoveGenerator.applyMoveUnchecked(node.state, valid)
        val child = node.addOrGetChild(valid, san, nextState)
        selectNode(child)
    }

    fun selectNode(node: MoveNode) {
        refreshDerivedForNode(node)
        if (_uiState.value.engineEnabled) {
            startEngineForCurrentNode()
        }
    }

    fun stepBack() {
        _uiState.value.currentNode.parent?.let { selectNode(it) }
    }

    fun stepForward() {
        _uiState.value.currentNode.children.firstOrNull()?.let { selectNode(it) }
    }

    fun jumpToStart() {
        selectNode(_uiState.value.rootNode)
    }

    fun jumpToEnd() {
        var n = _uiState.value.currentNode
        while (n.children.isNotEmpty()) n = n.children.first()
        selectNode(n)
    }

    fun jumpToPly(ply: Int) {
        val line = _uiState.value.rootNode.mainLine()
        if (ply <= 0) {
            jumpToStart()
        } else {
            line.getOrNull((ply - 1).coerceIn(0, line.lastIndex))?.let { selectNode(it) }
        }
    }

    fun setAutoplaySpeed(multiplier: Float) {
        val clamped = when {
            multiplier <= 0.6f -> 0.5f
            multiplier >= 1.8f -> 2.0f
            else -> 1.0f
        }
        val speedMs = when (clamped) {
            0.5f -> 1800L
            2.0f -> 450L
            else -> 900L
        }
        val wasPlaying = _uiState.value.isAutoPlaying
        _uiState.update { it.copy(autoplayMultiplier = clamped, autoplaySpeedMs = speedMs) }
        if (wasPlaying) {
            autoplayJob?.cancel()
            _uiState.update { it.copy(isAutoPlaying = false) }
            toggleAutoplay()
        }
    }

    fun toggleAutoplay() {
        if (_uiState.value.isAutoPlaying) {
            autoplayJob?.cancel()
            _uiState.update { it.copy(isAutoPlaying = false) }
        } else {
            _uiState.update { it.copy(isAutoPlaying = true) }
            autoplayJob = viewModelScope.launch {
                while (true) {
                    delay(_uiState.value.autoplaySpeedMs)
                    val next = _uiState.value.currentNode.children.firstOrNull()
                    if (next == null) {
                        _uiState.update { it.copy(isAutoPlaying = false) }
                        break
                    }
                    selectNode(next)
                }
            }
        }
    }

    fun setComment(text: String) {
        _uiState.value.currentNode.comment = text
        _uiState.update { it.copy(statusBanner = "Comment updated on ${_uiState.value.currentNode.san.ifBlank { "root" }}") }
    }

    fun toggleNag(nag: Int) {
        val nags = _uiState.value.currentNode.nags
        if (nag in nags) nags.remove(nag) else nags.add(nag)
        _uiState.update { it.copy(statusBanner = "Annotation updated") }
    }

    fun loadFen(fen: String, variant: ChessVariant = ChessVariant.STANDARD): Boolean {
        val res = ChessBoardState.fromFen(fen, variant)
        return res.fold(
            onSuccess = { state ->
                val root = MoveNode(state = state, move = null, san = "")
                _uiState.update {
                    it.copy(
                        rootNode = root,
                        currentNode = root,
                        reviewReport = null,
                        errorText = null,
                        userArrows = emptyList(),
                        userCircleSquares = emptySet(),
                        statusBanner = "Loaded FEN position",
                    )
                }
                refreshDerivedForNode(root)
                true
            },
            onFailure = { err ->
                _uiState.update { it.copy(errorText = err.message ?: "Invalid FEN") }
                false
            },
        )
    }

    fun importPgn(pgn: String): Boolean {
        val res = PgnParser.parseMultiGamePgn(pgn)
        return res.fold(
            onSuccess = { games ->
                val first = games.first()
                var endNode = first.rootNode
                while (endNode.children.isNotEmpty()) endNode = endNode.children.first()
                _uiState.update {
                    it.copy(
                        rootNode = first.rootNode,
                        currentNode = endNode,
                        reviewReport = null,
                        errorText = null,
                        userArrows = emptyList(),
                        userCircleSquares = emptySet(),
                        statusBanner = "Imported PGN (${games.size} game${if (games.size > 1) "s" else ""})",
                    )
                }
                refreshDerivedForNode(endNode)
                true
            },
            onFailure = { err ->
                _uiState.update { it.copy(errorText = err.message ?: "Malformed PGN") }
                false
            },
        )
    }

    fun loadSavedGame(game: LocalGameEntity) {
        if (!importPgn(game.pgn)) {
            loadFen(game.initialFen)
        }
    }

    fun exportPgn(): String = PgnParser.exportPgn(
        root = _uiState.value.rootNode,
        headers = mapOf("Event" to "Gamebit Analysis", "FEN" to _uiState.value.rootNode.state.toFen()),
        result = "*",
    )

    fun runFullGameReview() {
        val mainMoves = _uiState.value.rootNode.mainLine().mapNotNull { it.move }
        if (mainMoves.isEmpty()) {
            _uiState.update { it.copy(errorText = "Play or import moves first to run Game Review.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewing = true, errorText = null, statusBanner = "Analyzing ${mainMoves.size} moves...") }
            val engine = ChessEngine(currentSettings.lowMemoryMode)
            val report = engine.reviewGame(
                initialState = _uiState.value.rootNode.state,
                moves = mainMoves,
                useOpeningBook = currentSettings.openingBookEnabled,
            )

            // Attach evaluations and NAGs onto MoveNodes
            val nodes = _uiState.value.rootNode.mainLine()
            report.moves.forEachIndexed { idx, rm ->
                nodes.getOrNull(idx)?.let { node ->
                    node.evalCp = rm.evalAfterCp
                    if (rm.quality.nag > 0 && rm.quality.nag !in node.nags) {
                        node.nags.add(rm.quality.nag)
                    }
                }
            }

            // Convert blunders/mistakes into Learn-From-My-Mistakes custom drills
            report.moves.filter { it.isCriticalMoment && it.cpLoss >= 120 }.take(3).forEach { mistake ->
                localRepo.saveCustomPuzzle(
                    CustomPuzzleEntity(
                        id = "mistake_${mistake.fenBefore.hashCode()}",
                        title = "Fix Mistake (${mistake.san} -> ${mistake.bestMoveSan})",
                        fen = mistake.fenBefore,
                        solutionUci = mistake.bestMoveUci,
                        theme = "From My Games",
                        rating = 1350,
                    ),
                )
            }

            _uiState.update {
                it.copy(
                    isReviewing = false,
                    reviewReport = report,
                    statusBanner = "Review complete: W ${report.whiteAccuracy}% / B ${report.blackAccuracy}%",
                )
            }
        }
    }

    fun fetchOnlineExplorer(mastersOnly: Boolean) {
        val fen = _uiState.value.currentNode.state.toFen()
        viewModelScope.launch {
            _uiState.update { it.copy(externalExplorerLoading = true, errorText = null) }
            externalApis.queryOpeningExplorer(fen, mastersOnly).fold(
                onSuccess = { res ->
                    _uiState.update { it.copy(externalExplorer = res, externalExplorerLoading = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            externalExplorerLoading = false,
                            errorText = "Explorer offline/unavailable: ${err.message}",
                        )
                    }
                },
            )
        }
    }

    fun queryEndgameTablebase() {
        val state = _uiState.value.currentNode.state
        val pieces = state.board.count { it != null }
        if (pieces > 7) {
            _uiState.update { it.copy(errorText = "Syzygy tablebase supports positions with 7 or fewer pieces (currently $pieces).") }
            return
        }
        viewModelScope.launch {
            externalApis.queryTablebase(state.toFen()).fold(
                onSuccess = { tb -> _uiState.update { it.copy(tablebaseResult = tb, errorText = null) } },
                onFailure = { err -> _uiState.update { it.copy(errorText = "Tablebase query failed: ${err.message}") } },
            )
        }
    }

    fun saveAsLocalStudy(title: String, description: String) {
        val pgn = exportPgn()
        val chapter = StudyChapter(
            title = title.ifBlank { "Main Line" },
            initialFen = _uiState.value.rootNode.state.toFen(),
            pgn = pgn,
            notes = _uiState.value.currentNode.comment,
        )
        viewModelScope.launch {
            localRepo.saveStudy(
                LocalStudyEntity(
                    id = UUID.randomUUID().toString(),
                    title = title.ifBlank { "Saved Study" },
                    description = description,
                    tags = _uiState.value.openingMatch?.eco ?: "Analysis",
                    chaptersJson = LocalChessRepository.encodeChapters(listOf(chapter)),
                ),
            )
            _uiState.update { it.copy(statusBanner = "Saved to Studies") }
        }
    }

    private fun refreshDerivedForNode(node: MoveNode) {
        val path = node.pathFromRoot().drop(1)
        val ucis = path.mapNotNull { it.move?.uci }
        val localBook = if (node.state.variant == ChessVariant.STANDARD && currentSettings.openingBookEnabled) {
            OpeningBook.getLocalBookMoves(node.state, ucis)
        } else emptyList()
        val opening = if (node.state.variant == ChessVariant.STANDARD && currentSettings.openingRecognitionEnabled) {
            OpeningBook.recognize(ucis)
        } else null
        val gambit = if (node.state.variant == ChessVariant.STANDARD && currentSettings.gambitRecognitionEnabled) {
            OpeningBook.recognizeGambit(ucis)
        } else null
        val endgame = if (currentSettings.endgameRecognitionEnabled) EndgameClassifier.classify(node.state) else null
        val coach = ChessEngine(currentSettings.lowMemoryMode).explainCurrentPosition(node.state, null)
        _uiState.update {
            it.copy(
                currentNode = node,
                legalMoves = MoveGenerator.legalMoves(node.state),
                localBookMoves = localBook,
                openingMatch = opening,
                gambitMatch = gambit,
                endgameClassification = endgame,
                coachInsight = coach,
                tablebaseResult = null,
                userArrows = emptyList(),
                userCircleSquares = emptySet(),
            )
        }
    }

    private fun startEngineForCurrentNode() {
        engineJob?.cancel()
        val state = _uiState.value.currentNode.state
        val engine = ChessEngine(currentSettings.lowMemoryMode)
        val maxDepth = if (currentSettings.lowMemoryMode) currentSettings.engineDepth.coerceAtMost(3) else currentSettings.engineDepth.coerceIn(1, 5)
        val multiPv = if (currentSettings.lowMemoryMode) 1 else currentSettings.engineMultiPv.coerceIn(1, 3)

        engineJob = viewModelScope.launch {
            engine.analyzeStream(
                state = state,
                maxDepth = maxDepth,
                multiPv = multiPv,
            ).collect { lines ->
                val arrows = mutableListOf<BoardArrow>()
                if (currentSettings.showBestMoveArrow) {
                    lines.firstOrNull()?.pvMoves?.firstOrNull()?.let { best ->
                        arrows.add(BoardArrow(best.from, best.to, 0xCC10B981))
                    }
                }
                if (currentSettings.showThreatArrow) {
                    engine.computeOpponentThreat(state)?.let { threat ->
                        arrows.add(BoardArrow(threat.from, threat.to, 0xCCEF4444))
                    }
                }
                val coach = engine.explainCurrentPosition(state, lines.firstOrNull())
                _uiState.update {
                    it.copy(
                        engineLines = lines,
                        arrows = arrows,
                        coachInsight = coach,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopEngine()
    }
}

