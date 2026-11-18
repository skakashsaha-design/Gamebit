package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.ChessBoardState
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.data.local.ChessLesson
import com.example.data.local.CustomPuzzleEntity
import com.example.data.local.EndgameExercise
import com.example.data.local.LessonProgressEntity
import com.example.data.local.LocalChessRepository
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
    STREAK("Puzzle Streak"),
    STORM("Puzzle Storm"),
    RACER("Puzzle Racer"),
    MISTAKES("My Mistakes"),
}

data class PuzzleSessionState(
    val mode: PuzzleMode = PuzzleMode.RATED,
    val selectedTheme: String = "All",
    val currentPuzzle: TacticalPuzzle = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first(),
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
) : ViewModel() {
    private val _puzzleState = MutableStateFlow(PuzzleSessionState())
    val puzzleState: StateFlow<PuzzleSessionState> = _puzzleState.asStateFlow()

    private val _coordState = MutableStateFlow(CoordinateTrainerState())
    val coordState: StateFlow<CoordinateTrainerState> = _coordState.asStateFlow()

    val puzzleAttempts: StateFlow<List<PuzzleAttemptEntity>> = localRepo.observePuzzleAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPuzzles: StateFlow<List<CustomPuzzleEntity>> = localRepo.observeCustomPuzzles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessonProgress: StateFlow<List<LessonProgressEntity>> = localRepo.observeLessonProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var stormJob: Job? = null
    private var coordJob: Job? = null
    private var puzzleStartMs = System.currentTimeMillis()

    fun selectPuzzleMode(mode: PuzzleMode, theme: String = _puzzleState.value.selectedTheme) {
        stormJob?.cancel()
        if (mode == PuzzleMode.DAILY) {
            loadDailyPuzzle()
            return
        }
        if (mode == PuzzleMode.MISTAKES) {
            val custom = customPuzzles.value.firstOrNull()
            if (custom != null) {
                loadPuzzle(
                    TacticalPuzzle(
                        id = custom.id,
                        title = custom.title,
                        fen = custom.fen,
                        solutionUci = listOf(custom.solutionUci),
                        rating = custom.rating,
                        themes = listOf(custom.theme),
                        hint = "Correct the mistake from your analyzed game.",
                        explanation = "Found from your Game Review.",
                    ),
                    mode = PuzzleMode.MISTAKES,
                    theme = "From My Games",
                )
                return
            }
        }
        val pool = filteredPuzzles(theme)
        val picked = pool.randomOrNull() ?: PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        loadPuzzle(picked, mode = mode, theme = theme, resetCounters = true)
        if (mode == PuzzleMode.STORM || mode == PuzzleMode.RACER) {
            startStormTimer()
        }
    }

    fun selectThemeFilter(theme: String) {
        val pool = filteredPuzzles(theme)
        val picked = pool.randomOrNull() ?: PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        loadPuzzle(picked, mode = _puzzleState.value.mode, theme = theme)
    }

    private fun filteredPuzzles(theme: String): List<TacticalPuzzle> {
        if (theme == "All") return PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES
        return PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.filter { theme in it.themes }.ifEmpty {
            PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES
        }
    }

    fun nextPuzzle() {
        val curr = _puzzleState.value
        val pool = filteredPuzzles(curr.selectedTheme).filter { it.id != curr.currentPuzzle.id }
            .ifEmpty { PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES }
        loadPuzzle(pool.random(), mode = curr.mode, theme = curr.selectedTheme, resetCounters = false)
    }

    fun retryPuzzle() {
        val curr = _puzzleState.value
        loadPuzzle(curr.currentPuzzle, mode = curr.mode, theme = curr.selectedTheme, resetCounters = false)
    }

    fun revealHint() {
        _puzzleState.update { it.copy(showHint = true, feedbackText = it.currentPuzzle.hint) }
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

            _puzzleState.update {
                it.copy(
                    boardState = nextState,
                    legalMoves = if (isNowSolved) emptyList() else MoveGenerator.legalMoves(nextState),
                    solutionIndex = nextIdx,
                    solved = isNowSolved,
                    streakCount = newStreak,
                    stormScore = newStorm,
                    feedbackText = if (isNowSolved) "Solved! ${curr.currentPuzzle.explanation}" else "Good move! Keep going...",
                )
            }

            if (isNowSolved) {
                recordAttempt(curr.currentPuzzle, solved = true, streakNow = newStreak, stormNow = newStorm)
                if (curr.mode == PuzzleMode.STORM || curr.mode == PuzzleMode.RACER) {
                    viewModelScope.launch {
                        delay(450L)
                        nextPuzzle()
                    }
                }
            }
        } else {
            _puzzleState.update {
                it.copy(
                    failed = true,
                    streakCount = if (it.mode == PuzzleMode.STREAK) 0 else it.streakCount,
                    feedbackText = "Not quite — ${move.uci} misses the tactic. Tap Retry or Hint.",
                )
            }
            recordAttempt(curr.currentPuzzle, solved = false, streakNow = 0, stormNow = curr.stormScore)
        }
    }

    private fun recordAttempt(puzzle: TacticalPuzzle, solved: Boolean, streakNow: Int, stormNow: Int) {
        val elapsed = (System.currentTimeMillis() - puzzleStartMs).coerceAtLeast(500L)
        viewModelScope.launch {
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
                    loadPuzzle(daily, mode = PuzzleMode.DAILY, theme = "All", resetCounters = false)
                },
                onFailure = {
                    val offlineDaily = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
                    loadPuzzle(offlineDaily, mode = PuzzleMode.DAILY, theme = "All", resetCounters = false)
                    _puzzleState.update { it.copy(feedbackText = "Offline Daily Puzzle loaded.") }
                },
            )
        }
    }

    private fun loadPuzzle(
        puzzle: TacticalPuzzle,
        mode: PuzzleMode,
        theme: String,
        resetCounters: Boolean = false,
    ) {
        val state = ChessBoardState.fromFen(puzzle.fen).getOrElse { ChessBoardState.standard() }
        puzzleStartMs = System.currentTimeMillis()
        _puzzleState.update { prev ->
            PuzzleSessionState(
                mode = mode,
                selectedTheme = theme,
                currentPuzzle = puzzle,
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
