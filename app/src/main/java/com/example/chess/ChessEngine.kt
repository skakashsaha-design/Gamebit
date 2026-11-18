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
    BEST("Best", 1, "!"),
    EXCELLENT("Excellent", 1, ""),
    GOOD("Good", 0, ""),
    BOOK("Book", 0, "📖"),
    INACCURACY("Inaccuracy", 6, "?!"),
    MISTAKE("Mistake", 2, "?"),
    BLUNDER("Blunder", 4, "??"),
}

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
        var blackAccSum = 0.0
        var blackCount = 0
        var openingSum = 0.0
        var openingCount = 0
        var midSum = 0.0
        var midCount = 0
        var endSum = 0.0
        var endCount = 0

        var wInacc = 0
        var wMist = 0
        var wBlun = 0
        var wMissedWin = 0
        var wMissedMate = 0
        var bInacc = 0
        var bMist = 0
        var bBlun = 0
        var bMissedWin = 0
        var bMissedMate = 0

        val evalDepth = 2

        for ((idx, move) in moves.withIndex()) {
            currentCoroutineContext().ensureActive()
            val fenBefore = current.toFen()
            val san = MoveGenerator.toSan(current, move)
            val mover = current.sideToMove
            val isBookMove = useOpeningBook && OpeningBook.getLocalBookMoves(current, uciHistory).any { it.uci == move.uci }

            val bestLineBefore = evaluateBest(current, depth = evalDepth, timeLimitMs = 280L)
            val evalBeforeWhiteCp = bestLineBefore?.scoreCp ?: staticEvalWhite(current)
            val bestMove = bestLineBefore?.pvMoves?.firstOrNull() ?: move
            val bestSan = MoveGenerator.toSan(current, bestMove)

            val next = MoveGenerator.applyMoveUnchecked(current, move)
            val fenAfter = next.toFen()
            val lineAfter = evaluateBest(next, depth = evalDepth, timeLimitMs = 280L)
            val evalAfterWhiteCp = lineAfter?.scoreCp ?: staticEvalWhite(next)

            // Centipawn loss from mover's perspective
            val bestForMover = if (mover == PieceColor.WHITE) evalBeforeWhiteCp else -evalBeforeWhiteCp
            val afterForMover = if (mover == PieceColor.WHITE) evalAfterWhiteCp else -evalAfterWhiteCp
            val cpLoss = (bestForMover - afterForMover).coerceAtLeast(0)

            val winBefore = winProb(bestForMover)
            val winAfter = winProb(afterForMover)
            val winLoss = (winBefore - winAfter).coerceAtLeast(0.0)

            val isSacrifice = current.pieceAt(move.to) == null &&
                MoveGenerator.isSquareAttacked(next, move.to, mover.opposite) &&
                (current.pieceAt(move.from)?.type?.materialValue ?: 0) >= 300

            val quality = when {
                isBookMove && idx < 12 -> MoveQuality.BOOK
                move.uci == bestMove.uci && isSacrifice && cpLoss <= 20 && afterForMover >= -50 -> MoveQuality.BRILLIANT
                move.uci == bestMove.uci || cpLoss <= 12 -> MoveQuality.BEST
                winLoss <= 0.025 -> MoveQuality.EXCELLENT
                winLoss <= 0.055 -> MoveQuality.GOOD
                winLoss <= 0.11 -> MoveQuality.INACCURACY
                winLoss <= 0.22 -> MoveQuality.MISTAKE
                else -> MoveQuality.BLUNDER
            }

            when (quality) {
                MoveQuality.INACCURACY -> if (mover == PieceColor.WHITE) wInacc++ else bInacc++
                MoveQuality.MISTAKE -> if (mover == PieceColor.WHITE) wMist++ else bMist++
                MoveQuality.BLUNDER -> if (mover == PieceColor.WHITE) wBlun++ else bBlun++
                else -> Unit
            }

            // Detect missed mates & missed winning advantages
            if (bestForMover >= 45_000 && afterForMover < 45_000) {
                if (mover == PieceColor.WHITE) wMissedMate++ else bMissedMate++
            } else if (bestForMover >= 250 && afterForMover < 100 && move.uci != bestMove.uci) {
                if (mover == PieceColor.WHITE) wMissedWin++ else bMissedWin++
            }

            val moveAccuracy = (103.1668 * exp(-0.04354 * cpLoss.coerceAtMost(600)) - 3.1669).coerceIn(0.0, 100.0)
            if (mover == PieceColor.WHITE) {
                whiteAccSum += moveAccuracy
                whiteCount++
            } else {
                blackAccSum += moveAccuracy
                blackCount++
            }

            val pieceCount = next.board.count { it != null }
            when {
                idx < 14 -> { openingSum += moveAccuracy; openingCount++ }
                pieceCount <= 12 -> { endSum += moveAccuracy; endCount++ }
                else -> { midSum += moveAccuracy; midCount++ }
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
                playedSan = san,
                bestSan = bestSan,
                quality = quality,
                cpLoss = cpLoss,
            )

            reviewed.add(
                ReviewedMove(
                    ply = idx + 1,
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
                    isCriticalMoment = quality == MoveQuality.BLUNDER || quality == MoveQuality.MISTAKE || quality == MoveQuality.BRILLIANT,
                    mistakeBreakdown = breakdown,
                )
            )
            uciHistory.add(move.uci)
            current = next
        }

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
        )
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
        playedSan: String,
        bestSan: String,
        quality: MoveQuality,
        cpLoss: Int,
    ): String {
        val givesCheck = MoveGenerator.isKingInCheck(stateAfter, stateAfter.sideToMove)
        return when (quality) {
            MoveQuality.BOOK -> "$playedSan follows established opening theory."
            MoveQuality.BRILLIANT -> "$playedSan is a strong tactical sacrifice maintaining the advantage!"
            MoveQuality.BEST -> if (givesCheck) "$playedSan is the top engine move, seizing initiative with check." else "$playedSan is the strongest move in the position."
            MoveQuality.EXCELLENT, MoveQuality.GOOD -> "$playedSan keeps the position solid (best was $bestSan)."
            MoveQuality.INACCURACY -> "$playedSan slightly weakens the position (-${cpLoss} cp). $bestSan kept tighter control."
            MoveQuality.MISTAKE -> "$playedSan drops significant evaluation (-${cpLoss} cp). $bestSan was stronger."
            MoveQuality.BLUNDER -> "$playedSan loses major material or tactical ground (-${cpLoss} cp). Prefer $bestSan."
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
