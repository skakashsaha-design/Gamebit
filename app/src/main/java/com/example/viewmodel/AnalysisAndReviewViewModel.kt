package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.BoardArrow
import com.example.chess.BookMoveStat
import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessVariant
import com.example.chess.EngineLine
import com.example.chess.GameReviewReport
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveNode
import com.example.chess.OpeningBook
import com.example.chess.OpeningMatch
import com.example.chess.PgnParser
import com.example.data.local.AppSettings
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
    val coachInsight: String = "",
    val isAutoPlaying: Boolean = false,
    val autoplaySpeedMs: Long = 900L,
    val reviewReport: GameReviewReport? = null,
    val isReviewing: Boolean = false,
    val statusBanner: String = "",
    val errorText: String? = null,
    val arrows: List<BoardArrow> = emptyList(),
)

class AnalysisAndReviewViewModel(
    private val localRepo: LocalChessRepository,
    private val externalApis: ExternalChessApis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var engineJob: Job? = null
    private var autoplayJob: Job? = null
    private var currentSettings = AppSettings()

    init {
        refreshDerivedForNode(_uiState.value.rootNode)
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
        headers = mapOf("Event" to "ChessHub Analysis", "FEN" to _uiState.value.rootNode.state.toFen()),
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
            val report = engine.reviewGame(_uiState.value.rootNode.state, mainMoves)

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
        val localBook = if (node.state.variant == ChessVariant.STANDARD) {
            OpeningBook.getLocalBookMoves(node.state, ucis)
        } else emptyList()
        val opening = if (node.state.variant == ChessVariant.STANDARD) OpeningBook.recognize(ucis) else null
        val coach = ChessEngine(currentSettings.lowMemoryMode).explainCurrentPosition(node.state, null)
        _uiState.update {
            it.copy(
                currentNode = node,
                legalMoves = MoveGenerator.legalMoves(node.state),
                localBookMoves = localBook,
                openingMatch = opening,
                coachInsight = coach,
                tablebaseResult = null,
            )
        }
    }

    private fun startEngineForCurrentNode() {
        engineJob?.cancel()
        val state = _uiState.value.currentNode.state
        val engine = ChessEngine(currentSettings.lowMemoryMode)
        engineJob = viewModelScope.launch {
            engine.analyzeStream(
                state = state,
                maxDepth = if (currentSettings.lowMemoryMode) 3 else 4,
                multiPv = if (currentSettings.lowMemoryMode) 1 else 3,
            ).collect { lines ->
                val arrows = mutableListOf<BoardArrow>()
                lines.firstOrNull()?.pvMoves?.firstOrNull()?.let { best ->
                    arrows.add(BoardArrow(best.from, best.to, 0xCC10B981))
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
