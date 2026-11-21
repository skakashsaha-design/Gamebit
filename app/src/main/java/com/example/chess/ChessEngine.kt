package com.example.chess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

data class EngineLine(
    val multipvRank: Int,
    val scoreCp: Int, // Positive = White advantage, Negative = Black advantage
    val mateIn: Int?, // Positive = White mates in N, Negative = Black mates in N
    val depth: Int,
    val nodes: Long,
    val nps: Long,
    val pvMoves: List<Move>,
    val pvSan: List<String>,
) {
    val formattedScore: String
        get() = when {
            mateIn != null -> if (mateIn > 0) "+M$mateIn" else "-M${abs(mateIn)}"
            else -> {
                val pawns = scoreCp / 100.0
                if (pawns >= 0) String.format(java.util.Locale.US, "+%.2f", pawns)
                else String.format(java.util.Locale.US, "%.2f", pawns)
            }
        }

    val whiteWinProbability: Float
        get() {
            if (mateIn != null) return if (mateIn > 0) 1.0f else 0.0f
            val cp = scoreCp.coerceIn(-1500, 1500)
            return (1.0 / (1.0 + exp(-0.00368208 * cp))).toFloat()
        }
}

enum class MoveQuality(val label: String, val nag: Int, val symbol: String) {
    BRILLIANT("Brilliant", 3, "!!"),
    GREAT("Great", 1, "!"),
    BEST("Best", 1, "★"),
    EXCELLENT("Excellent", 0, "✓"),
    GOOD("Good", 0, "•"),
    BOOK("Book", 0, "📖"),
    FORCED("Forced", 0, "□"),
    INACCURACY("Inaccuracy", 6, "?!"),
    MISTAKE("Mistake", 2, "?"),
    MISS("Miss", 2, "✕"),
    BLUNDER("Blunder", 4, "??"),
}

data class TurningPoint(
    val ply: Int,
    val san: String,
    val mover: PieceColor,
    val quality: MoveQuality,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val swingCp: Int,
    val title: String,
    val description: String,
)

data class ReviewedMove(
    val ply: Int,
    val san: String,
    val uci: String,
    val fenBefore: String,
    val fenAfter: String,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val bestMoveSan: String,
    val bestMoveUci: String,
    val cpLoss: Int,
    val quality: MoveQuality,
    val coachExplanation: String,
    val isCriticalMoment: Boolean,
    val mistakeBreakdown: MistakeAnalysisBreakdown? = null,
    val mateAfter: Int? = null,
)

data class GameReviewReport(
    val moves: List<ReviewedMove>,
    val whiteAccuracy: Int,
    val blackAccuracy: Int,
    val openingAccuracy: Int,
    val middlegameAccuracy: Int,
    val endgameAccuracy: Int,
    val whiteInaccuracies: Int,
    val whiteMistakes: Int,
    val whiteBlunders: Int,
    val whiteMissedWins: Int = 0,
    val whiteMissedMates: Int = 0,
    val blackInaccuracies: Int,
    val blackMistakes: Int,
    val blackBlunders: Int,
    val blackMissedWins: Int = 0,
    val blackMissedMates: Int = 0,
    val whiteOpeningAccuracy: Int? = null,
    val whiteMiddlegameAccuracy: Int? = null,
    val whiteEndgameAccuracy: Int? = null,
    val blackOpeningAccuracy: Int? = null,
    val blackMiddlegameAccuracy: Int? = null,
    val blackEndgameAccuracy: Int? = null,
    val whiteAcpl: Int = 0,
    val blackAcpl: Int = 0,
    val whiteQualityCounts: Map<MoveQuality, Int> = emptyMap(),
    val blackQualityCounts: Map<MoveQuality, Int> = emptyMap(),
    val whiteBestMove: ReviewedMove? = null,
    val blackBestMove: ReviewedMove? = null,
    val whiteWorstMove: ReviewedMove? = null,
    val blackWorstMove: ReviewedMove? = null,
    val whiteMissedOpportunity: ReviewedMove? = null,
    val blackMissedOpportunity: ReviewedMove? = null,
    val turningPoints: List<TurningPoint> = emptyList(),
    val advantageSummary: String = "",
)

class ChessEngine(
    private val lowMemoryMode: Boolean = false,
) {
    private val maxTableEntries: Int get() = if (lowMemoryMode) 8_000 else 32_000
    private val transpositionTable = LinkedHashMap<String, TTEntry>(1024, 0.75f, true)

    private data class TTEntry(val depth: Int, val score: Int, val bestMove: Move?)

    private var nodesSearched = 0L
    private var startTimeMs = 0L
    private var timeBudgetMs = 1500L

    // Piece-Square Tables (from White's perspective; flipped for Black)
    private val pawnPst = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, -20, -20, 10, 10, 5,
        5, -5, -10, 0, 0, -10, -5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, 5, 10, 25, 25, 10, 5, 5,
        10, 10, 20, 30, 30, 20, 10, 10,
        50, 50, 50, 50, 50, 50, 50, 50,
        0, 0, 0, 0, 0, 0, 0, 0,
    )
    private val knightPst = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50,
    )
    private val bishopPst = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -20, -10, -10, -10, -10, -10, -10, -20,
    )
    private val kingMidPst = intArrayOf(
        20, 30, 10, 0, 0, 10, 30, 20,
        20, 20, 0, 0, 0, 0, 20, 20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
    )

    fun analyzeStream(
        state: ChessBoardState,
        maxDepth: Int = if (lowMemoryMode) 3 else 4,
        multiPv: Int = if (lowMemoryMode) 1 else 3,
        timeLimitMs: Long = if (lowMemoryMode) 1000L else 1800L,
    ): Flow<List<EngineLine>> = flow {
        val effectiveMaxDepth = if (lowMemoryMode) maxDepth.coerceAtMost(3) else maxDepth.coerceIn(1, 6)
        val effectiveMultiPv = if (lowMemoryMode) 1 else multiPv.coerceIn(1, 3)

        val legal = MoveGenerator.legalMoves(state)
        if (legal.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        nodesSearched = 0L
        startTimeMs = System.currentTimeMillis()
        timeBudgetMs = timeLimitMs

        for (depth in 1..effectiveMaxDepth) {
            currentCoroutineContext().ensureActive()
            val lines = searchMultiPv(state, legal, depth, effectiveMultiPv)
            if (lines.isNotEmpty()) {
                emit(lines)
            }
            if (System.currentTimeMillis() - startTimeMs >= timeBudgetMs) break
        }
    }.flowOn(Dispatchers.Default)

    suspend fun evaluateBest(
        state: ChessBoardState,
        depth: Int = if (lowMemoryMode) 2 else 3,
        timeLimitMs: Long = 900L,
    ): EngineLine? = withContext(Dispatchers.Default) {
        val legal = MoveGenerator.legalMoves(state)
        if (legal.isEmpty()) return@withContext null
        nodesSearched = 0L
        startTimeMs = System.currentTimeMillis()
        timeBudgetMs = timeLimitMs
        var best: List<EngineLine> = emptyList()
        val capDepth = if (lowMemoryMode) depth.coerceAtMost(2) else depth.coerceIn(1, 4)
        for (d in 1..capDepth) {
            currentCoroutineContext().ensureActive()
            best = searchMultiPv(state, legal, d, 1)
            if (System.currentTimeMillis() - startTimeMs >= timeBudgetMs) break
        }
        best.firstOrNull()
    }

    suspend fun evaluateTopTwo(
        state: ChessBoardState,
        depth: Int = if (lowMemoryMode) 2 else 3,
        timeLimitMs: Long = 450L,
    ): List<EngineLine> = withContext(Dispatchers.Default) {
        val legal = MoveGenerator.legalMoves(state)
        if (legal.isEmpty()) return@withContext emptyList()
        nodesSearched = 0L
        startTimeMs = System.currentTimeMillis()
        timeBudgetMs = timeLimitMs
        var best: List<EngineLine> = emptyList()
        val capDepth = if (lowMemoryMode) depth.coerceAtMost(2) else depth.coerceIn(1, 3)
        for (d in 1..capDepth) {
            currentCoroutineContext().ensureActive()
            best = searchMultiPv(state, legal, d, 2)
            if (System.currentTimeMillis() - startTimeMs >= timeBudgetMs) break
        }
        best
    }

    suspend fun chooseBotMove(
        state: ChessBoardState,
        bot: BotProfile,
        playedUciMoves: List<String>,
        useOpeningBook: Boolean = true,
    ): Move? = withContext(Dispatchers.Default) {
        val legal = MoveGenerator.legalMoves(state)
        if (legal.isEmpty()) return@withContext null

        if (useOpeningBook && state.variant == ChessVariant.STANDARD && Random.nextFloat() < (1f - bot.bookRandomness * 0.4f)) {
            val bookMoves = OpeningBook.getLocalBookMoves(state, playedUciMoves)
            if (bookMoves.isNotEmpty()) {
                val chosenBook = if (Random.nextFloat() < bot.bookRandomness) bookMoves.random() else bookMoves.first()
                val from = Square.fromAlgebraic(chosenBook.uci.take(2))
                val to = Square.fromAlgebraic(chosenBook.uci.drop(2).take(2))
                val matched = legal.firstOrNull { it.from == from && it.to == to }
                if (matched != null) return@withContext matched
            }
        }

        nodesSearched = 0L
        startTimeMs = System.currentTimeMillis()
        timeBudgetMs = if (lowMemoryMode) 650L else 1200L
        val targetDepth = if (lowMemoryMode) bot.searchDepth.coerceAtMost(3) else bot.searchDepth.coerceIn(1, 4)

        val scoredMoves = legal.map { move ->
            currentCoroutineContext().ensureActive()
            val next = MoveGenerator.applyMoveUnchecked(state, move)
            val rawScoreFromMover = -alphaBeta(next, targetDepth - 1, -100_000, 100_000, state.sideToMove.opposite)
            val noise = if (bot.randomnessCentipawns > 0) {
                Random.nextInt(-bot.randomnessCentipawns, bot.randomnessCentipawns + 1)
            } else 0
            val captureBonus = if (state.pieceAt(move.to) != null) (25f * (bot.aggressionWeight - 1f)).toInt() else 0
            move to (rawScoreFromMover + noise + captureBonus)
        }.sortedByDescending { it.second }

        scoredMoves.firstOrNull()?.first
    }

    suspend fun computeOpponentThreat(state: ChessBoardState): Move? = withContext(Dispatchers.Default) {
        if (MoveGenerator.isKingInCheck(state, state.sideToMove)) return@withContext null
        val nullState = state.copy(sideToMove = state.sideToMove.opposite, enPassantSquare = null)
        evaluateBest(nullState, depth = 2, timeLimitMs = 350L)?.pvMoves?.firstOrNull()
    }

    suspend fun evaluateSingleMove(
        stateBefore: ChessBoardState,
        move: Move,
        historyUciBefore: List<String>,
        useOpeningBook: Boolean = true,
    ): ReviewedMove = withContext(Dispatchers.Default) {
        evaluateMoveInternal(
            ply = historyUciBefore.size + 1,
            current = stateBefore,
            move = move,
            uciHistory = historyUciBefore,
            useOpeningBook = useOpeningBook,
            evalDepth = 2,
            timePerSearchMs = 190L,
        )
    }

    suspend fun reviewGame(
        initialState: ChessBoardState,
        moves: List<Move>,
        useOpeningBook: Boolean = true,
    ): GameReviewReport = withContext(Dispatchers.Default) {
        val reviewed = mutableListOf<ReviewedMove>()
        var current = initialState
        val uciHistory = mutableListOf<String>()

        var whiteAccSum = 0.0
        var whiteCount = 0
        var whiteCpLossSum = 0L
        var blackAccSum = 0.0
        var blackCount = 0
        var blackCpLossSum = 0L

        var openingSum = 0.0
        var openingCount = 0
        var midSum = 0.0
        var midCount = 0
        var endSum = 0.0
        var endCount = 0

        var wOpenSum = 0.0; var wOpenCount = 0
        var wMidSum = 0.0; var wMidCount = 0
        var wEndSum = 0.0; var wEndCount = 0
        var bOpenSum = 0.0; var bOpenCount = 0
        var bMidSum = 0.0; var bMidCount = 0
        var bEndSum = 0.0; var bEndCount = 0

        val wCounts = mutableMapOf<MoveQuality, Int>()
        val bCounts = mutableMapOf<MoveQuality, Int>()

        var wInacc = 0; var wMist = 0; var wBlun = 0; var wMissedWin = 0; var wMissedMate = 0
        var bInacc = 0; var bMist = 0; var bBlun = 0; var bMissedWin = 0; var bMissedMate = 0

        val evalDepth = 2

        for ((idx, move) in moves.withIndex()) {
            currentCoroutineContext().ensureActive()
            val mover = current.sideToMove
            val rm = evaluateMoveInternal(
                ply = idx + 1,
                current = current,
                move = move,
                uciHistory = uciHistory,
                useOpeningBook = useOpeningBook,
                evalDepth = evalDepth,
                timePerSearchMs = 260L,
            )
            reviewed.add(rm)

            val countsMap = if (mover == PieceColor.WHITE) wCounts else bCounts
            countsMap[rm.quality] = (countsMap[rm.quality] ?: 0) + 1

            when (rm.quality) {
                MoveQuality.INACCURACY -> if (mover == PieceColor.WHITE) wInacc++ else bInacc++
                MoveQuality.MISTAKE -> if (mover == PieceColor.WHITE) wMist++ else bMist++
                MoveQuality.BLUNDER -> if (mover == PieceColor.WHITE) wBlun++ else bBlun++
                MoveQuality.MISS -> {
                    val bestForMover = if (mover == PieceColor.WHITE) rm.evalBeforeCp else -rm.evalBeforeCp
                    val afterForMover = if (mover == PieceColor.WHITE) rm.evalAfterCp else -rm.evalAfterCp
                    if (bestForMover >= 45_000 && afterForMover < 45_000) {
                        if (mover == PieceColor.WHITE) wMissedMate++ else bMissedMate++
                    } else {
                        if (mover == PieceColor.WHITE) wMissedWin++ else bMissedWin++
                    }
                }
                else -> Unit
            }

            val moveAccuracy = (103.1668 * exp(-0.04354 * rm.cpLoss.coerceAtMost(600)) - 3.1669).coerceIn(0.0, 100.0)
            val next = MoveGenerator.applyMoveUnchecked(current, move)
            val pieceCount = next.board.count { it != null }
            val isOpeningPhase = idx < 14
            val isEndgamePhase = !isOpeningPhase && (pieceCount <= 12 || EndgameClassifier.classify(next) != null)

            if (mover == PieceColor.WHITE) {
                whiteAccSum += moveAccuracy
                whiteCount++
                whiteCpLossSum += rm.cpLoss
                when {
                    isOpeningPhase -> { wOpenSum += moveAccuracy; wOpenCount++ }
                    isEndgamePhase -> { wEndSum += moveAccuracy; wEndCount++ }
                    else -> { wMidSum += moveAccuracy; wMidCount++ }
                }
            } else {
                blackAccSum += moveAccuracy
                blackCount++
                blackCpLossSum += rm.cpLoss
                when {
                    isOpeningPhase -> { bOpenSum += moveAccuracy; bOpenCount++ }
                    isEndgamePhase -> { bEndSum += moveAccuracy; bEndCount++ }
                    else -> { bMidSum += moveAccuracy; bMidCount++ }
                }
            }

            when {
                isOpeningPhase -> { openingSum += moveAccuracy; openingCount++ }
                isEndgamePhase -> { endSum += moveAccuracy; endCount++ }
                else -> { midSum += moveAccuracy; midCount++ }
            }

            uciHistory.add(move.uci)
            current = next
        }

        val whiteMoves = reviewed.filterIndexed { index, _ -> index % 2 == 0 }
        val blackMoves = reviewed.filterIndexed { index, _ -> index % 2 == 1 }

        val whiteBest = whiteMoves.firstOrNull { it.quality == MoveQuality.BRILLIANT }
            ?: whiteMoves.firstOrNull { it.quality == MoveQuality.GREAT }
            ?: whiteMoves.filter { it.quality == MoveQuality.BEST }.minByOrNull { it.cpLoss }
            ?: whiteMoves.minByOrNull { it.cpLoss }

        val blackBest = blackMoves.firstOrNull { it.quality == MoveQuality.BRILLIANT }
            ?: blackMoves.firstOrNull { it.quality == MoveQuality.GREAT }
            ?: blackMoves.filter { it.quality == MoveQuality.BEST }.minByOrNull { it.cpLoss }
            ?: blackMoves.minByOrNull { it.cpLoss }

        val whiteWorst = whiteMoves.filter {
            it.quality == MoveQuality.BLUNDER || it.quality == MoveQuality.MISTAKE || it.quality == MoveQuality.INACCURACY
        }.maxByOrNull { it.cpLoss }
        val blackWorst = blackMoves.filter {
            it.quality == MoveQuality.BLUNDER || it.quality == MoveQuality.MISTAKE || it.quality == MoveQuality.INACCURACY
        }.maxByOrNull { it.cpLoss }

        val whiteMissed = whiteMoves.filter { it.quality == MoveQuality.MISS }.maxByOrNull { it.cpLoss }
        val blackMissed = blackMoves.filter { it.quality == MoveQuality.MISS }.maxByOrNull { it.cpLoss }

        val turningPoints = buildTurningPoints(reviewed)
        val advantageSummary = buildAdvantageSummary(reviewed)

        GameReviewReport(
            moves = reviewed,
            whiteAccuracy = if (whiteCount > 0) (whiteAccSum / whiteCount).roundToInt() else 100,
            blackAccuracy = if (blackCount > 0) (blackAccSum / blackCount).roundToInt() else 100,
            openingAccuracy = if (openingCount > 0) (openingSum / openingCount).roundToInt() else 100,
            middlegameAccuracy = if (midCount > 0) (midSum / midCount).roundToInt() else 100,
            endgameAccuracy = if (endCount > 0) (endSum / endCount).roundToInt() else 100,
            whiteInaccuracies = wInacc,
            whiteMistakes = wMist,
            whiteBlunders = wBlun,
            whiteMissedWins = wMissedWin,
            whiteMissedMates = wMissedMate,
            blackInaccuracies = bInacc,
            blackMistakes = bMist,
            blackBlunders = bBlun,
            blackMissedWins = bMissedWin,
            blackMissedMates = bMissedMate,
            whiteOpeningAccuracy = if (wOpenCount >= 2) (wOpenSum / wOpenCount).roundToInt() else null,
            whiteMiddlegameAccuracy = if (wMidCount >= 2) (wMidSum / wMidCount).roundToInt() else null,
            whiteEndgameAccuracy = if (wEndCount >= 2) (wEndSum / wEndCount).roundToInt() else null,
            blackOpeningAccuracy = if (bOpenCount >= 2) (bOpenSum / bOpenCount).roundToInt() else null,
            blackMiddlegameAccuracy = if (bMidCount >= 2) (bMidSum / bMidCount).roundToInt() else null,
            blackEndgameAccuracy = if (bEndCount >= 2) (bEndSum / bEndCount).roundToInt() else null,
            whiteAcpl = if (whiteCount > 0) (whiteCpLossSum.toDouble() / whiteCount).roundToInt() else 0,
            blackAcpl = if (blackCount > 0) (blackCpLossSum.toDouble() / blackCount).roundToInt() else 0,
            whiteQualityCounts = wCounts,
            blackQualityCounts = bCounts,
            whiteBestMove = whiteBest,
            blackBestMove = blackBest,
            whiteWorstMove = whiteWorst,
            blackWorstMove = blackWorst,
            whiteMissedOpportunity = whiteMissed,
            blackMissedOpportunity = blackMissed,
            turningPoints = turningPoints,
            advantageSummary = advantageSummary,
        )
    }

    private suspend fun evaluateMoveInternal(
        ply: Int,
        current: ChessBoardState,
        move: Move,
        uciHistory: List<String>,
        useOpeningBook: Boolean,
        evalDepth: Int,
        timePerSearchMs: Long,
    ): ReviewedMove {
        val fenBefore = current.toFen()
        val legalBefore = MoveGenerator.legalMoves(current)
        val san = MoveGenerator.toSan(current, move, legalBefore)
        val mover = current.sideToMove
        val isForced = legalBefore.size == 1
        val isBookMove = useOpeningBook &&
            current.variant == ChessVariant.STANDARD &&
            ply <= 16 &&
            OpeningBook.isBookMove(uciHistory, move.uci)

        val topLinesBefore = evaluateTopTwo(current, depth = evalDepth, timeLimitMs = timePerSearchMs)
        val bestLineBefore = topLinesBefore.firstOrNull()
        val secondLineBefore = topLinesBefore.getOrNull(1)
        val evalBeforeWhiteCp = bestLineBefore?.scoreCp ?: staticEvalWhite(current)
        val bestMove = bestLineBefore?.pvMoves?.firstOrNull() ?: move
        val bestSan = MoveGenerator.toSan(current, bestMove, legalBefore)

        val next = MoveGenerator.applyMoveUnchecked(current, move)
        val fenAfter = next.toFen()
        val (statusAfter, _) = MoveGenerator.evaluateGameResult(next)
        val lineAfter = if (statusAfter.isTerminal) null else evaluateBest(next, depth = evalDepth, timeLimitMs = timePerSearchMs)
        val evalAfterWhiteCp = when {
            statusAfter == GameStatus.CHECKMATE -> if (mover == PieceColor.WHITE) 50_000 else -50_000
            statusAfter.isTerminal -> 0
            else -> lineAfter?.scoreCp ?: staticEvalWhite(next)
        }
        val mateAfter = when {
            statusAfter == GameStatus.CHECKMATE -> if (mover == PieceColor.WHITE) 1 else -1
            else -> lineAfter?.mateIn
        }

        val bestForMover = if (mover == PieceColor.WHITE) evalBeforeWhiteCp else -evalBeforeWhiteCp
        val afterForMover = if (mover == PieceColor.WHITE) evalAfterWhiteCp else -evalAfterWhiteCp
        val cpLoss = (bestForMover - afterForMover).coerceAtLeast(0)

        val secondForMover = secondLineBefore?.let {
            if (mover == PieceColor.WHITE) it.scoreCp else -it.scoreCp
        }
        val secondBestGapCp = if (secondForMover != null) (bestForMover - secondForMover).coerceAtLeast(0) else 0

        val winBefore = winProb(bestForMover)
        val winAfter = winProb(afterForMover)
        val winLoss = (winBefore - winAfter).coerceAtLeast(0.0)

        val isSacrifice = current.pieceAt(move.to) == null &&
            MoveGenerator.isSquareAttacked(next, move.to, mover.opposite) &&
            (current.pieceAt(move.from)?.type?.materialValue ?: 0) >= 300

        val missedMate = bestForMover >= 45_000 && afterForMover < 45_000 && move.uci != bestMove.uci
        val missedClearWin = bestForMover >= 250 && afterForMover <= 90 && move.uci != bestMove.uci && winLoss > 0.08

        val quality = when {
            isForced -> MoveQuality.FORCED
            isBookMove -> MoveQuality.BOOK
            move.uci == bestMove.uci && isSacrifice && cpLoss <= 20 && afterForMover >= -50 -> MoveQuality.BRILLIANT
            move.uci == bestMove.uci && secondBestGapCp >= 110 && afterForMover >= -40 -> MoveQuality.GREAT
            move.uci == bestMove.uci || cpLoss <= 12 -> MoveQuality.BEST
            missedMate || missedClearWin -> MoveQuality.MISS
            winLoss <= 0.025 -> MoveQuality.EXCELLENT
            winLoss <= 0.055 -> MoveQuality.GOOD
            winLoss <= 0.11 -> MoveQuality.INACCURACY
            winLoss <= 0.22 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }

        val breakdown = MistakeAnalyzer.analyze(
            stateBefore = current,
            stateAfter = next,
            movePlayed = move,
            playedSan = san,
            bestMoveSan = bestSan,
            evalBeforeWhiteCp = evalBeforeWhiteCp,
            evalAfterWhiteCp = evalAfterWhiteCp,
            cpLoss = cpLoss,
            quality = quality,
        )

        val coachNote = buildCoachExplanation(
            stateBefore = current,
            stateAfter = next,
            movePlayed = move,
            playedSan = san,
            bestSan = bestSan,
            quality = quality,
            cpLoss = cpLoss,
        )

        return ReviewedMove(
            ply = ply,
            san = san,
            uci = move.uci,
            fenBefore = fenBefore,
            fenAfter = fenAfter,
            evalBeforeCp = evalBeforeWhiteCp,
            evalAfterCp = evalAfterWhiteCp,
            bestMoveSan = bestSan,
            bestMoveUci = bestMove.uci,
            cpLoss = cpLoss,
            quality = quality,
            coachExplanation = coachNote,
            isCriticalMoment = quality == MoveQuality.BLUNDER ||
                quality == MoveQuality.MISTAKE ||
                quality == MoveQuality.MISS ||
                quality == MoveQuality.BRILLIANT ||
                quality == MoveQuality.GREAT,
            mistakeBreakdown = breakdown,
            mateAfter = mateAfter,
        )
    }

    private fun buildTurningPoints(reviewed: List<ReviewedMove>): List<TurningPoint> {
        if (reviewed.isEmpty()) return emptyList()
        val points = mutableListOf<TurningPoint>()
        var whiteBestMoment: TurningPoint? = null
        var blackBestMoment: TurningPoint? = null

        reviewed.forEach { rm ->
            val mover = if (rm.ply % 2 == 1) PieceColor.WHITE else PieceColor.BLACK
            val swing = rm.evalAfterCp - rm.evalBeforeCp
            val absSwing = abs(swing)
            if (rm.quality == MoveQuality.BLUNDER || rm.quality == MoveQuality.MISTAKE ||
                rm.quality == MoveQuality.MISS || rm.quality == MoveQuality.BRILLIANT ||
                rm.quality == MoveQuality.GREAT || absSwing >= 160
            ) {
                val moveNum = (rm.ply + 1) / 2
                val prefix = if (mover == PieceColor.WHITE) "$moveNum." else "$moveNum..."
                val label = when {
                    rm.evalAfterCp >= 150 && rm.evalBeforeCp < 150 -> "White took control"
                    rm.evalAfterCp <= -150 && rm.evalBeforeCp > -150 -> "Black took control"
                    rm.quality == MoveQuality.BRILLIANT || rm.quality == MoveQuality.GREAT -> "Sharp breakthrough"
                    rm.quality == MoveQuality.MISS -> "Missed decisive chance"
                    rm.quality == MoveQuality.BLUNDER -> "Game-changing blunder"
                    else -> "Key turning point"
                }
                val tp = TurningPoint(
                    ply = rm.ply,
                    san = rm.san,
                    mover = mover,
                    quality = rm.quality,
                    evalBeforeCp = rm.evalBeforeCp,
                    evalAfterCp = rm.evalAfterCp,
                    swingCp = swing,
                    title = "$prefix ${rm.san} — $label",
                    description = rm.coachExplanation,
                )
                points.add(tp)
            }
            if (rm.evalAfterCp >= 120 && (whiteBestMoment == null || rm.evalAfterCp > whiteBestMoment!!.evalAfterCp)) {
                val moveNum = (rm.ply + 1) / 2
                whiteBestMoment = TurningPoint(
                    ply = rm.ply,
                    san = rm.san,
                    mover = mover,
                    quality = rm.quality,
                    evalBeforeCp = rm.evalBeforeCp,
                    evalAfterCp = rm.evalAfterCp,
                    swingCp = swing,
                    title = "Where White stood best (Move $moveNum)",
                    description = "Evaluation reached ${formatEvalCompact(rm.evalAfterCp, rm.mateAfter)} after ${rm.san}.",
                )
            }
            if (rm.evalAfterCp <= -120 && (blackBestMoment == null || rm.evalAfterCp < blackBestMoment!!.evalAfterCp)) {
                val moveNum = (rm.ply + 1) / 2
                blackBestMoment = TurningPoint(
                    ply = rm.ply,
                    san = rm.san,
                    mover = mover,
                    quality = rm.quality,
                    evalBeforeCp = rm.evalBeforeCp,
                    evalAfterCp = rm.evalAfterCp,
                    swingCp = swing,
                    title = "Where Black stood best (Move $moveNum)",
                    description = "Evaluation reached ${formatEvalCompact(rm.evalAfterCp, rm.mateAfter)} after ${rm.san}.",
                )
            }
        }
        val combined = mutableListOf<TurningPoint>()
        whiteBestMoment?.let { combined.add(it) }
        blackBestMoment?.let { combined.add(it) }
        points.sortedByDescending { abs(it.swingCp) + it.quality.nag * 50 }.take(5).forEach { tp ->
            if (combined.none { it.ply == tp.ply }) combined.add(tp)
        }
        return combined.sortedBy { it.ply }
    }

    private fun buildAdvantageSummary(reviewed: List<ReviewedMove>): String {
        if (reviewed.isEmpty()) return "No moves played yet."
        val whitePeak = reviewed.maxByOrNull { it.evalAfterCp }
        val blackPeak = reviewed.minByOrNull { it.evalAfterCp }
        val wBetter = whitePeak != null && whitePeak.evalAfterCp >= 100
        val bBetter = blackPeak != null && blackPeak.evalAfterCp <= -100
        return when {
            wBetter && bBetter ->
                "Swinging battle: White peaked on ply ${whitePeak!!.ply} (${formatEvalCompact(whitePeak.evalAfterCp, whitePeak.mateAfter)}) and Black peaked on ply ${blackPeak!!.ply} (${formatEvalCompact(blackPeak.evalAfterCp, blackPeak.mateAfter)})."
            wBetter ->
                "White held the initiative, peaking after move ${(whitePeak!!.ply + 1) / 2} (${whitePeak.san}) at ${formatEvalCompact(whitePeak.evalAfterCp, whitePeak.mateAfter)}."
            bBetter ->
                "Black held the initiative, peaking after move ${(blackPeak!!.ply + 1) / 2} (${blackPeak.san}) at ${formatEvalCompact(blackPeak.evalAfterCp, blackPeak.mateAfter)}."
            else -> "Closely balanced game where neither side broke through cleanly."
        }
    }

    private fun formatEvalCompact(cp: Int, mateIn: Int?): String = when {
        mateIn != null -> if (mateIn > 0) "+M$mateIn" else "-M${abs(mateIn)}"
        abs(cp) >= 45_000 -> if (cp > 0) "+M" else "-M"
        else -> {
            val pawns = cp / 100.0
            if (pawns >= 0) String.format(java.util.Locale.US, "+%.1f", pawns)
            else String.format(java.util.Locale.US, "%.1f", pawns)
        }
    }

    fun explainCurrentPosition(state: ChessBoardState, bestLine: EngineLine?): String {
        val checkSide = if (MoveGenerator.isKingInCheck(state, state.sideToMove)) {
            "${if (state.sideToMove == PieceColor.WHITE) "White" else "Black"} is in check. "
        } else ""
        val materialCp = materialBalanceWhite(state)
        val matText = when {
            materialCp >= 200 -> "White leads in material by +${materialCp / 100.0} pawns."
            materialCp <= -200 -> "Black leads in material by +${abs(materialCp) / 100.0} pawns."
            else -> "Material is roughly balanced."
        }
        val bestText = bestLine?.let { line ->
            val top = line.pvSan.firstOrNull() ?: return@let ""
            " Best engine continuation: $top (${line.formattedScore} at depth ${line.depth})."
        } ?: ""
        return "$checkSide$matText$bestText".trim()
    }

    private fun buildCoachExplanation(
        stateBefore: ChessBoardState,
        stateAfter: ChessBoardState,
        movePlayed: Move,
        playedSan: String,
        bestSan: String,
        quality: MoveQuality,
        cpLoss: Int,
    ): String {
        val givesCheck = MoveGenerator.isKingInCheck(stateAfter, stateAfter.sideToMove)
        val wasCapture = stateBefore.pieceAt(movePlayed.to) != null || movePlayed.isEnPassant
        val movedPiece = stateBefore.pieceAt(movePlayed.from)?.type
        val detailSuffix = when {
            movePlayed.isCastling -> " It tucks the king to safety and brings a rook into play."
            givesCheck -> " It pressures the opponent's king with check."
            wasCapture -> " It wins or trades material cleanly."
            movedPiece == PieceType.PAWN && movePlayed.to.file in 2..5 && movePlayed.to.rank in 2..5 ->
                " It stakes a claim in the center."
            else -> ""
        }
        return when (quality) {
            MoveQuality.BOOK -> "$playedSan is an established opening book move, developing pieces smoothly.$detailSuffix"
            MoveQuality.FORCED -> "$playedSan was your only legal move in this position."
            MoveQuality.BRILLIANT -> "$playedSan is a brilliant sacrifice! It creates serious tactical threats while keeping or increasing your advantage."
            MoveQuality.GREAT -> "$playedSan is a great critical find—the only move that preserves control and keeps pressure on the opponent.$detailSuffix"
            MoveQuality.BEST -> "$playedSan is the top engine choice in this position.$detailSuffix"
            MoveQuality.EXCELLENT -> "$playedSan is an accurate, strong move keeping your position healthy (engine slightly preferred $bestSan).$detailSuffix"
            MoveQuality.GOOD -> "$playedSan is a solid, playable move (strongest continuation was $bestSan).$detailSuffix"
            MoveQuality.INACCURACY -> "$playedSan is a small inaccuracy (-${String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)} pawns). $bestSan kept tighter control over key squares."
            MoveQuality.MISTAKE -> "$playedSan is a mistake (-${String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)} pawns) that weakens coordination or concedes an initiative. Prefer $bestSan."
            MoveQuality.MISS -> "$playedSan missed a golden opportunity! $bestSan would have pressed a winning attack or decisive advantage."
            MoveQuality.BLUNDER -> "$playedSan is a critical blunder (-${String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)} pawns) allowing a major tactical swing. $bestSan was needed."
        }
    }

    private fun winProb(moverCp: Int): Double = 1.0 / (1.0 + exp(-0.00368208 * moverCp.coerceIn(-1500, 1500)))

    private suspend fun searchMultiPv(
        state: ChessBoardState,
        legal: List<Move>,
        depth: Int,
        multiPv: Int,
    ): List<EngineLine> {
        val ordered = orderMoves(state, legal, transpositionTable[state.positionKey()]?.bestMove)
        val results = mutableListOf<EngineLine>()

        for (move in ordered) {
            currentCoroutineContext().ensureActive()
            if (System.currentTimeMillis() - startTimeMs >= timeBudgetMs && results.isNotEmpty()) break
            val next = MoveGenerator.applyMoveUnchecked(state, move)
            val scoreFromSide = -alphaBeta(next, depth - 1, -100_000, 100_000, state.sideToMove.opposite)
            val whiteScore = if (state.sideToMove == PieceColor.WHITE) scoreFromSide else -scoreFromSide
            val mateIn = detectMatePlies(whiteScore)
            val san = MoveGenerator.toSan(state, move, legal)
            val pvFollow = extractPvFromTT(next, depth - 1)
            val elapsedMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)
            val nps = (nodesSearched * 1000L) / elapsedMs

            results.add(
                EngineLine(
                    multipvRank = 0,
                    scoreCp = whiteScore,
                    mateIn = mateIn,
                    depth = depth,
                    nodes = nodesSearched,
                    nps = nps,
                    pvMoves = listOf(move) + pvFollow.first,
                    pvSan = listOf(san) + pvFollow.second,
                )
            )
        }

        val sorted = if (state.sideToMove == PieceColor.WHITE) {
            results.sortedByDescending { it.scoreCp }
        } else {
            results.sortedBy { it.scoreCp }
        }

        return sorted.take(multiPv).mapIndexed { index, line ->
            line.copy(multipvRank = index + 1)
        }
    }

    private fun detectMatePlies(whiteScore: Int): Int? {
        val absScore = abs(whiteScore)
        if (absScore < 45_000) return null
        val plies = (50_000 - absScore).coerceAtLeast(1)
        val moves = (plies + 1) / 2
        return if (whiteScore > 0) moves else -moves
    }

    private fun extractPvFromTT(startState: ChessBoardState, maxLen: Int): Pair<List<Move>, List<String>> {
        val moves = mutableListOf<Move>()
        val sans = mutableListOf<String>()
        var curr = startState
        val seen = mutableSetOf<String>()
        repeat(maxLen.coerceAtMost(6)) {
            val key = curr.positionKey()
            if (!seen.add(key)) return moves to sans
            val ttMove = transpositionTable[key]?.bestMove ?: return moves to sans
            val legal = MoveGenerator.legalMoves(curr)
            val valid = legal.firstOrNull { it.uci == ttMove.uci } ?: return moves to sans
            sans.add(MoveGenerator.toSan(curr, valid, legal))
            moves.add(valid)
            curr = MoveGenerator.applyMoveUnchecked(curr, valid)
        }
        return moves to sans
    }

    private suspend fun alphaBeta(
        state: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        beta: Int,
        side: PieceColor,
    ): Int {
        nodesSearched++
        if ((nodesSearched and 255L) == 0L) {
            currentCoroutineContext().ensureActive()
            if (System.currentTimeMillis() - startTimeMs >= timeBudgetMs) {
                return staticEvalForSide(state, side)
            }
        }

        val key = state.positionKey()
        val tt = transpositionTable[key]
        if (tt != null && tt.depth >= depth) {
            return tt.score
        }

        val legal = MoveGenerator.legalMoves(state)
        if (legal.isEmpty()) {
            return when (state.variant) {
                ChessVariant.ANTICHESS -> 49_000 - (10 - depth)
                else -> {
                    if (MoveGenerator.isKingInCheck(state, side)) {
                        -(50_000 - (10 - depth))
                    } else 0
                }
            }
        }

        if (depth <= 0) {
            return quiescence(state, alphaInit, beta, side, qDepth = 2)
        }

        var alpha = alphaInit
        var bestScore = -100_000
        var bestMove: Move? = null
        val ordered = orderMoves(state, legal, tt?.bestMove)

        for (move in ordered) {
            val next = MoveGenerator.applyMoveUnchecked(state, move)
            val score = -alphaBeta(next, depth - 1, -beta, -alpha, side.opposite)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }

        if (transpositionTable.size >= maxTableEntries) {
            val firstKey = transpositionTable.keys.firstOrNull()
            if (firstKey != null) transpositionTable.remove(firstKey)
        }
        transpositionTable[key] = TTEntry(depth, bestScore, bestMove)
        return bestScore
    }

    private fun quiescence(
        state: ChessBoardState,
        alphaInit: Int,
        beta: Int,
        side: PieceColor,
        qDepth: Int,
    ): Int {
        nodesSearched++
        val standPat = staticEvalForSide(state, side)
        if (qDepth <= 0 || standPat >= beta) return standPat
        var alpha = maxOf(alphaInit, standPat)

        val captures = MoveGenerator.legalMoves(state).filter {
            state.pieceAt(it.to) != null || it.promotion != null
        }
        for (move in orderMoves(state, captures, null)) {
            val next = MoveGenerator.applyMoveUnchecked(state, move)
            val score = -quiescence(next, -beta, -alpha, side.opposite, qDepth - 1)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(state: ChessBoardState, moves: List<Move>, hashMove: Move?): List<Move> {
        if (moves.size <= 1) return moves
        return moves.sortedByDescending { move ->
            var score = 0
            if (hashMove != null && move.uci == hashMove.uci) score += 10_000
            val victim = state.pieceAt(move.to)
            val attacker = state.pieceAt(move.from)
            if (victim != null && attacker != null) {
                score += 1000 + victim.type.materialValue - (attacker.type.materialValue / 10)
            }
            if (move.promotion != null) score += 800 + move.promotion.materialValue
            if (move.isCastling) score += 120
            score
        }
    }

    private fun staticEvalForSide(state: ChessBoardState, side: PieceColor): Int {
        val whiteEval = staticEvalWhite(state)
        return if (side == PieceColor.WHITE) whiteEval else -whiteEval
    }

    private fun materialBalanceWhite(state: ChessBoardState): Int {
        var score = 0
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.type == PieceType.KING) continue
            val v = p.type.materialValue
            score += if (p.color == PieceColor.WHITE) v else -v
        }
        return score
    }

    private fun staticEvalWhite(state: ChessBoardState): Int {
        if (state.variant == ChessVariant.ANTICHESS) {
            return -materialBalanceWhite(state)
        }
        var score = 0
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            val rank = i shr 3
            val file = i and 7
            val pstIdx = if (p.color == PieceColor.WHITE) i else ((7 - rank) shl 3) or file
            val pst = when (p.type) {
                PieceType.PAWN -> pawnPst[pstIdx]
                PieceType.KNIGHT -> knightPst[pstIdx]
                PieceType.BISHOP -> bishopPst[pstIdx]
                PieceType.KING -> kingMidPst[pstIdx]
                PieceType.ROOK -> if (rank == 6 && p.color == PieceColor.WHITE || rank == 1 && p.color == PieceColor.BLACK) 15 else 0
                PieceType.QUEEN -> 0
            }
            val value = p.type.materialValue + pst
            score += if (p.color == PieceColor.WHITE) value else -value
        }
        if (state.variant == ChessVariant.KING_OF_THE_HILL) {
            MoveGenerator.findKingSquare(state, PieceColor.WHITE)?.let { sq ->
                score += (4 - maxOf(abs(sq.file - 3), abs(sq.rank - 3))) * 35
            }
            MoveGenerator.findKingSquare(state, PieceColor.BLACK)?.let { sq ->
                score -= (4 - maxOf(abs(sq.file - 3), abs(sq.rank - 3))) * 35
            }
        }
        return score
    }
}
