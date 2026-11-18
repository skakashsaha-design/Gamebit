package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceCoachManager
import com.example.chess.ChessBoardState
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.data.local.AppSettings
import com.example.data.local.ChessLesson
import com.example.data.local.ChessPuzzleEntity
import com.example.data.local.CustomPuzzleEntity
import com.example.data.local.LessonProgressEntity
import com.example.data.local.LocalChessRepository
import com.example.data.local.OpeningDrill
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.data.local.SettingsRepository
import com.example.data.local.TacticalPuzzle
import com.example.data.remote.ExternalChessApis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

enum class PuzzleMode(val label: String) {
    DAILY("Daily Puzzle"),
    RATED("Rated Tactics"),
    CHECKMATE_PATTERNS("Checkmate Patterns"),
    STREAK("Puzzle Streak"),
    STORM("Puzzle Storm"),
    RACER("Puzzle Racer"),
    BOOKMARKS("Bookmarks"),
    MISTAKES("My Mistakes / Custom"),
}

data class PuzzleSessionState(
    val mode: PuzzleMode = PuzzleMode.RATED,
    val selectedTheme: String = "All",
    val selectedDifficultyBand: String = "All", // All, <1000, 1000-1399, 1400+
    val currentPuzzle: TacticalPuzzle = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first(),
    val isCurrentBookmarked: Boolean = false,
    val boardState: ChessBoardState = ChessBoardState.fromFen(currentPuzzle.fen).getOrThrow(),
    val legalMoves: List<Move> = MoveGenerator.legalMoves(boardState),
    val solutionIndex: Int = 0,
    val humanColor: PieceColor = boardState.sideToMove,
    val solved: Boolean = false,
    val failed: Boolean = false,
    val showHint: Boolean = false,
    val streakCount: Int = 0,
    val stormScore: Int = 0,
    val stormRemainingSec: Int = 180,
    val feedbackText: String = "Find the best move!",
    val loadingDaily: Boolean = false,
)

data class OpeningDrillSessionState(
    val drill: OpeningDrill = PuzzlesAndLessonsCatalog.OPENING_DRILLS.first(),
    val boardState: ChessBoardState = ChessBoardState.standard(),
    val legalMoves: List<Move> = MoveGenerator.legalMoves(ChessBoardState.standard()),
    val moveIndex: Int = 0,
    val completed: Boolean = false,
    val feedback: String = "Play the next theory move in the drill.",
)

data class CoordinateTrainerState(
    val targetSquare: String = "e4",
    val flipped: Boolean = false,
    val active: Boolean = false,
    val timeRemainingSec: Int = 30,
    val score: Int = 0,
    val attempts: Int = 0,
    val lastResultCorrect: Boolean? = null,
)

class TrainingViewModel(
    private val localRepo: LocalChessRepository,
    private val settingsRepo: SettingsRepository,
    private val externalApis: ExternalChessApis,
    private val voiceCoach: VoiceCoachManager? = null,
) : ViewModel() {
    private val _puzzleState = MutableStateFlow(PuzzleSessionState())
    val puzzleState: StateFlow<PuzzleSessionState> = _puzzleState.asStateFlow()

    private val _drillState = MutableStateFlow(OpeningDrillSessionState())
    val drillState: StateFlow<OpeningDrillSessionState> = _drillState.asStateFlow()

    private val _coordState = MutableStateFlow(CoordinateTrainerState())
    val coordState: StateFlow<CoordinateTrainerState> = _coordState.asStateFlow()

    val puzzleCollection: StateFlow<List<ChessPuzzleEntity>> = localRepo.observePuzzleCollection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puzzleAttempts: StateFlow<List<PuzzleAttemptEntity>> = localRepo.observePuzzleAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPuzzles: StateFlow<List<CustomPuzzleEntity>> = localRepo.observeCustomPuzzles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessonProgress: StateFlow<List<LessonProgressEntity>> = localRepo.observeLessonProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var stormJob: Job? = null
    private var coordJob: Job? = null
    private var puzzleStartMs = System.currentTimeMillis()
    private var currentSettings = AppSettings()

    init {
        viewModelScope.launch {
            localRepo.seedBuiltInPuzzlesIfNeeded()
        }
        startOpeningDrill(PuzzlesAndLessonsCatalog.OPENING_DRILLS.first())
    }

    fun updateSettings(settings: AppSettings) {
        currentSettings = settings
    }

    fun selectPuzzleMode(mode: PuzzleMode, theme: String = _puzzleState.value.selectedTheme) {
        stormJob?.cancel()
        if (mode == PuzzleMode.DAILY) {
            loadDailyPuzzle()
            return
        }
        val effectiveTheme = if (mode == PuzzleMode.CHECKMATE_PATTERNS) "Checkmate Pattern" else theme
        val pool = poolForModeAndFilters(mode, effectiveTheme, _puzzleState.value.selectedDifficultyBand)
        val picked = pool.randomOrNull() ?: PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        loadPuzzle(picked, mode = mode, theme = effectiveTheme, band = _puzzleState.value.selectedDifficultyBand, resetCounters = true)
        if (mode == PuzzleMode.STORM || mode == PuzzleMode.RACER) {
            startStormTimer()
        }
    }

    fun selectThemeFilter(theme: String) {
        val curr = _puzzleState.value
        val pool = poolForModeAndFilters(curr.mode, theme, curr.selectedDifficultyBand)
        val picked = pool.randomOrNull() ?: PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        loadPuzzle(picked, mode = curr.mode, theme = theme, band = curr.selectedDifficultyBand)
    }

    fun selectDifficultyBand(band: String) {
        val curr = _puzzleState.value
        val pool = poolForModeAndFilters(curr.mode, curr.selectedTheme, band)
        val picked = pool.randomOrNull() ?: PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        loadPuzzle(picked, mode = curr.mode, theme = curr.selectedTheme, band = band)
    }

    fun loadSpecificPuzzle(puzzle: TacticalPuzzle, mode: PuzzleMode = _puzzleState.value.mode) {
        loadPuzzle(puzzle, mode = mode, theme = _puzzleState.value.selectedTheme, band = _puzzleState.value.selectedDifficultyBand, resetCounters = false)
    }

    private fun allAvailablePuzzles(): List<TacticalPuzzle> {
        val dbPuzzles = puzzleCollection.value.map { it.toTacticalPuzzle() }
        return if (dbPuzzles.isNotEmpty()) dbPuzzles else PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES
    }

    private fun poolForModeAndFilters(mode: PuzzleMode, theme: String, band: String): List<TacticalPuzzle> {
        val all = allAvailablePuzzles()
        val dbEntities = puzzleCollection.value
        val byMode = when (mode) {
            PuzzleMode.CHECKMATE_PATTERNS -> all.filter { it.isCheckmatePattern }
            PuzzleMode.BOOKMARKS -> {
                val bmIds = dbEntities.filter { it.bookmarked }.map { it.id }.toSet()
                all.filter { it.id in bmIds }.ifEmpty { all }
            }
            PuzzleMode.MISTAKES -> {
                val customFromDb = dbEntities.filter { it.source == "custom" || it.source == "mistake" }.map { it.toTacticalPuzzle() }
                val legacyCustom = customPuzzles.value.map { c ->
                    TacticalPuzzle(
                        id = c.id,
                        title = c.title,
                        fen = c.fen,
                        solutionUci = c.solutionUci.split(" ").filter { it.isNotBlank() },
                        rating = c.rating,
                        themes = listOf(c.theme),
                        hint = "Correct the mistake from your analyzed game or custom position.",
                        explanation = "Custom / Game Review puzzle.",
                    )
                }
                (customFromDb + legacyCustom).distinctBy { it.id }.ifEmpty { all }
            }
            else -> all
        }

        val byTheme = if (theme == "All" || theme == "Checkmate Pattern") {
            byMode
        } else {
            byMode.filter { p -> p.themes.any { it.equals(theme, ignoreCase = true) } || p.patternName.equals(theme, ignoreCase = true) }
        }

        val byBand = when (band) {
            "<1000" -> byTheme.filter { it.rating < 1000 }
            "1000-1399" -> byTheme.filter { it.rating in 1000..1399 }
            "1400+" -> byTheme.filter { it.rating >= 1400 }
            else -> byTheme
        }

        return byBand.ifEmpty { byTheme.ifEmpty { byMode.ifEmpty { all } } }
    }

    fun nextPuzzle() {
        val curr = _puzzleState.value
        val pool = poolForModeAndFilters(curr.mode, curr.selectedTheme, curr.selectedDifficultyBand)
            .filter { it.id != curr.currentPuzzle.id }
            .ifEmpty { poolForModeAndFilters(curr.mode, curr.selectedTheme, curr.selectedDifficultyBand) }
        loadPuzzle(pool.random(), mode = curr.mode, theme = curr.selectedTheme, band = curr.selectedDifficultyBand, resetCounters = false)
    }

    fun retryPuzzle() {
        val curr = _puzzleState.value
        loadPuzzle(curr.currentPuzzle, mode = curr.mode, theme = curr.selectedTheme, band = curr.selectedDifficultyBand, resetCounters = false)
    }

    fun revealHint() {
        val hint = _puzzleState.value.currentPuzzle.hint
        _puzzleState.update { it.copy(showHint = true, feedbackText = hint) }
        voiceCoach?.speakTrainer(hint, currentSettings)
    }

    fun toggleBookmarkCurrentPuzzle() {
        val id = _puzzleState.value.currentPuzzle.id
        val next = !_puzzleState.value.isCurrentBookmarked
        _puzzleState.update { it.copy(isCurrentBookmarked = next) }
        viewModelScope.launch {
            localRepo.togglePuzzleBookmark(id, next)
        }
    }

    fun createCustomPuzzle(
        title: String,
        fen: String,
        solutionMoves: String,
        rating: Int,
        theme: String,
        hint: String,
        explanation: String,
    ): Boolean {
        val parsed = ChessBoardState.fromFen(fen)
        if (parsed.isFailure || solutionMoves.trim().isBlank()) return false
        viewModelScope.launch {
            val entity = ChessPuzzleEntity(
                id = "custom_${UUID.randomUUID().toString().take(12)}",
                title = title.trim().ifBlank { "Custom Puzzle" },
                fen = fen.trim(),
                solutionMoves = solutionMoves.trim(),
                difficultyRating = rating.coerceIn(400, 3000),
                themes = theme.trim().ifBlank { "Custom" },
                hint = hint.trim().ifBlank { "Calculate forcing checks and captures first." },
                explanation = explanation.trim().ifBlank { "User-created ChessHub puzzle." },
                source = "custom",
            )
            localRepo.savePuzzle(entity)
            loadPuzzle(entity.toTacticalPuzzle(), mode = PuzzleMode.MISTAKES, theme = "All", band = "All")
        }
        return true
    }

    fun deletePuzzle(id: String) {
        viewModelScope.launch {
            localRepo.deletePuzzle(id)
        }
    }

    fun submitPuzzleMove(move: Move) {
        val curr = _puzzleState.value
        if (curr.solved || curr.failed) return
        val expectedUci = curr.currentPuzzle.solutionUci.getOrNull(curr.solutionIndex) ?: return

        if (move.uci == expectedUci || (expectedUci.length == 4 && move.uci.startsWith(expectedUci))) {
            var nextState = MoveGenerator.applyMoveUnchecked(curr.boardState, move)
            var nextIdx = curr.solutionIndex + 1

            // Auto-play opponent's forced response if puzzle has multiple plies
            if (nextIdx < curr.currentPuzzle.solutionUci.size) {
                val replyUci = curr.currentPuzzle.solutionUci[nextIdx]
                val replyMove = MoveGenerator.legalMoves(nextState).firstOrNull { it.uci == replyUci }
                if (replyMove != null) {
                    nextState = MoveGenerator.applyMoveUnchecked(nextState, replyMove)
                    nextIdx++
                }
            }

            val isNowSolved = nextIdx >= curr.currentPuzzle.solutionUci.size
            val newStreak = if (isNowSolved && curr.mode == PuzzleMode.STREAK) curr.streakCount + 1 else curr.streakCount
            val newStorm = if (isNowSolved && (curr.mode == PuzzleMode.STORM || curr.mode == PuzzleMode.RACER)) curr.stormScore + 1 else curr.stormScore
            val feedback = if (isNowSolved) "Solved! ${curr.currentPuzzle.explanation}" else "Good move! Keep going..."

            _puzzleState.update {
                it.copy(
                    boardState = nextState,
                    legalMoves = if (isNowSolved) emptyList() else MoveGenerator.legalMoves(nextState),
                    solutionIndex = nextIdx,
                    solved = isNowSolved,
                    streakCount = newStreak,
                    stormScore = newStorm,
                    feedbackText = feedback,
                )
            }

            if (isNowSolved) {
                voiceCoach?.speakTrainer("Solved! ${curr.currentPuzzle.explanation}", currentSettings)
                recordAttempt(curr.currentPuzzle, solved = true, streakNow = newStreak, stormNow = newStorm)
                if (curr.mode == PuzzleMode.STORM || curr.mode == PuzzleMode.RACER) {
                    viewModelScope.launch {
                        delay(450L)
                        nextPuzzle()
                    }
                }
            }
        } else {
            val failText = "Not quite — ${move.uci} misses the tactic. Tap Retry or Hint."
            _puzzleState.update {
                it.copy(
                    failed = true,
                    streakCount = if (it.mode == PuzzleMode.STREAK) 0 else it.streakCount,
                    feedbackText = failText,
                )
            }
            voiceCoach?.speakTrainer("Not quite. Try again or tap hint.", currentSettings)
            recordAttempt(curr.currentPuzzle, solved = false, streakNow = 0, stormNow = curr.stormScore)
        }
    }

    private fun recordAttempt(puzzle: TacticalPuzzle, solved: Boolean, streakNow: Int, stormNow: Int) {
        val elapsed = (System.currentTimeMillis() - puzzleStartMs).coerceAtLeast(500L)
        viewModelScope.launch {
            localRepo.recordPuzzleSolveStats(puzzle.id, solved)
            localRepo.recordPuzzleAttempt(
                PuzzleAttemptEntity(
                    id = UUID.randomUUID().toString(),
                    puzzleId = puzzle.id,
                    theme = puzzle.themes.firstOrNull() ?: "Tactics",
                    puzzleRating = puzzle.rating,
                    solved = solved,
                    timeTakenMs = elapsed,
                    mode = _puzzleState.value.mode.name.lowercase(),
                ),
            )
            settingsRepo.recordPuzzleResult(puzzle.rating, solved, streakNow, stormNow)
        }
    }

    private fun loadDailyPuzzle() {
        _puzzleState.update { it.copy(mode = PuzzleMode.DAILY, loadingDaily = true, feedbackText = "Fetching Daily Puzzle...") }
        viewModelScope.launch {
            externalApis.fetchLichessDailyPuzzle().fold(
                onSuccess = { daily ->
                    loadPuzzle(daily, mode = PuzzleMode.DAILY, theme = "All", band = "All", resetCounters = false)
                },
                onFailure = {
                    val offlineDaily = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
                    loadPuzzle(offlineDaily, mode = PuzzleMode.DAILY, theme = "All", band = "All", resetCounters = false)
                    _puzzleState.update { it.copy(feedbackText = "Offline Daily Puzzle loaded.") }
                },
            )
        }
    }

    private fun loadPuzzle(
        puzzle: TacticalPuzzle,
        mode: PuzzleMode,
        theme: String,
        band: String = "All",
        resetCounters: Boolean = false,
    ) {
        val state = ChessBoardState.fromFen(puzzle.fen).getOrElse { ChessBoardState.standard() }
        puzzleStartMs = System.currentTimeMillis()
        val isBm = puzzleCollection.value.firstOrNull { it.id == puzzle.id }?.bookmarked ?: false
        _puzzleState.update { prev ->
            PuzzleSessionState(
                mode = mode,
                selectedTheme = theme,
                selectedDifficultyBand = band,
                currentPuzzle = puzzle,
                isCurrentBookmarked = isBm,
                boardState = state,
                legalMoves = MoveGenerator.legalMoves(state),
                solutionIndex = 0,
                humanColor = state.sideToMove,
                solved = false,
                failed = false,
                showHint = false,
                streakCount = if (resetCounters) 0 else prev.streakCount,
                stormScore = if (resetCounters) 0 else prev.stormScore,
                stormRemainingSec = if (resetCounters) 180 else prev.stormRemainingSec,
                feedbackText = "${if (state.sideToMove == PieceColor.WHITE) "White" else "Black"} to move — find the tactic.",
                loadingDaily = false,
            )
        }
    }

    private fun startStormTimer() {
        stormJob?.cancel()
        stormJob = viewModelScope.launch {
            while (_puzzleState.value.stormRemainingSec > 0) {
                delay(1000L)
                _puzzleState.update { it.copy(stormRemainingSec = (it.stormRemainingSec - 1).coerceAtLeast(0)) }
            }
            _puzzleState.update { it.copy(feedbackText = "Time's up! Final score: ${it.stormScore}") }
        }
    }

    // --- Opening & Gambit Drill ---
    fun startOpeningDrill(drill: OpeningDrill) {
        val fullLine = drill.lineMovesUci + drill.expectedMoveUci
        var st = ChessBoardState.standard()
        // Auto-play setup lineMovesUci so user plays the signature opening/gambit strike next
        for (uci in drill.lineMovesUci) {
            val m = MoveGenerator.legalMoves(st).firstOrNull { it.uci == uci } ?: break
            st = MoveGenerator.applyMoveUnchecked(st, m)
        }
        if (st.toFen() != drill.targetFen) {
            st = ChessBoardState.fromFen(drill.targetFen).getOrElse { st }
        }
        _drillState.value = OpeningDrillSessionState(
            drill = drill,
            boardState = st,
            legalMoves = MoveGenerator.legalMoves(st),
            moveIndex = drill.lineMovesUci.size.coerceAtMost(fullLine.size),
            completed = false,
            feedback = "${drill.title} (${drill.eco}): ${drill.keyIdea}",
        )
    }

    fun submitDrillMove(move: Move) {
        val curr = _drillState.value
        if (curr.completed) return
        val expected = curr.drill.expectedMoveUci
        if (move.uci == expected) {
            val next = MoveGenerator.applyMoveUnchecked(curr.boardState, move)
            val msg = "Drill complete! ${curr.drill.keyIdea}"
            _drillState.update {
                it.copy(
                    boardState = next,
                    legalMoves = emptyList(),
                    moveIndex = curr.drill.lineMovesUci.size + 1,
                    completed = true,
                    feedback = msg,
                )
            }
            voiceCoach?.speakTrainer(msg, currentSettings)
        } else {
            _drillState.update { it.copy(feedback = "Expected theory move $expected — try again!") }
        }
    }

    fun markLessonStepCompleted(lesson: ChessLesson, stepIdx: Int) {
        viewModelScope.launch {
            val done = stepIdx + 1 >= lesson.steps.size
            localRepo.saveLessonProgress(
                LessonProgressEntity(
                    lessonId = lesson.id,
                    completedSteps = (stepIdx + 1).coerceAtMost(lesson.steps.size),
                    totalSteps = lesson.steps.size,
                    completed = done,
                    downloadedOffline = true,
                ),
            )
        }
    }

    // --- Coordinate Trainer ---
    fun startCoordinateTrainer(flipped: Boolean) {
        coordJob?.cancel()
        _coordState.value = CoordinateTrainerState(
            targetSquare = randomSquareName(),
            flipped = flipped,
            active = true,
            timeRemainingSec = 30,
            score = 0,
            attempts = 0,
        )
        coordJob = viewModelScope.launch {
            while (_coordState.value.timeRemainingSec > 0 && _coordState.value.active) {
                delay(1000L)
                _coordState.update { it.copy(timeRemainingSec = (it.timeRemainingSec - 1).coerceAtLeast(0)) }
            }
            _coordState.update { it.copy(active = false) }
        }
    }

    fun onCoordinateSquareTapped(sq: Square) {
        val curr = _coordState.value
        if (!curr.active) return
        val correct = sq.algebraic == curr.targetSquare
        _coordState.update {
            it.copy(
                score = if (correct) it.score + 1 else it.score,
                attempts = it.attempts + 1,
                lastResultCorrect = correct,
                targetSquare = if (correct) randomSquareName() else it.targetSquare,
            )
        }
    }

    private fun randomSquareName(): String {
        val f = 'a' + Random.nextInt(8)
        val r = '1' + Random.nextInt(8)
        return "$f$r"
    }

    override fun onCleared() {
        super.onCleared()
        stormJob?.cancel()
        coordJob?.cancel()
    }
}

