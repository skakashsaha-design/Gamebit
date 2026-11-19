package com.example.chess

import com.example.data.local.EndgameExercise
import kotlin.math.abs

data class EndgameMoveVerdict(
    val playedSan: String,
    val bestSan: String,
    val quality: MoveQuality,
    val cpLoss: Int,
    val isMistake: Boolean,
)

data class EndgameCoachReport(
    val classificationTitle: String,
    val evalFormatted: String,
    val evalWhiteCp: Int,
    val moveVerdict: EndgameMoveVerdict?,
    val mistakeExplanation: String,
    val weakPoints: List<String>,
    val advantageSummary: String,
    val tips: List<String>,
    val bestMoveUci: String?,
    val bestMoveSan: String?,
)

/**
 * Pure-Kotlin Endgame Trainer coaching analyzer providing concrete diagnostics for:
 * 1. Mistake explanation (with MoveQuality & cpLoss)
 * 2. Weak point detection (king passivity, isolated/doubled pawns, bad bishop, rook blocking pawn, stalemate traps)
 * 3. Advantage breakdown (eval, material, passed pawns, king centralization, activity)
 * 4. Actionable Endgame Tips tailored to the position and exercise
 */
object EndgameCoach {

    suspend fun analyzePosition(
        state: ChessBoardState,
        exercise: EndgameExercise? = null,
        lowMemoryMode: Boolean = false,
        searchDepth: Int = if (lowMemoryMode) 2 else 3,
    ): EndgameCoachReport {
        val engine = ChessEngine(lowMemoryMode = lowMemoryMode)
        val bestLine = engine.evaluateBest(state, depth = searchDepth, timeLimitMs = 650L)
        val evalWhiteCp = lineToWhiteCp(state.sideToMove, bestLine)
        val bestMove = bestLine?.pvMoves?.firstOrNull()
        val bestSan = bestLine?.pvSan?.firstOrNull()
            ?: bestMove?.let { MoveGenerator.toSan(state, it, MoveGenerator.legalMoves(state)) }

        val classification = EndgameClassifier.classify(state)
        val classTitle = classification?.let { "${it.category} — ${it.detail}" }
            ?: (exercise?.category ?: "Technical Endgame")

        return EndgameCoachReport(
            classificationTitle = classTitle,
            evalFormatted = formatWhiteCp(evalWhiteCp, bestLine?.mateIn, state.sideToMove),
            evalWhiteCp = evalWhiteCp,
            moveVerdict = null,
            mistakeExplanation = exercise?.let {
                "Exercise Goal: ${it.objective} Play ${bestSan ?: it.bestStartUci} or test a plan on the board."
            } ?: "Play a move to get immediate mistake, weak point, advantage, and tip analysis.",
            weakPoints = detectWeakPoints(state, classification),
            advantageSummary = buildAdvantageSummary(state, evalWhiteCp, classification),
            tips = buildTips(state, classification, exercise, bestSan),
            bestMoveUci = bestMove?.uci,
            bestMoveSan = bestSan,
        )
    }

    suspend fun analyzeMove(
        stateBefore: ChessBoardState,
        movePlayed: Move,
        stateAfter: ChessBoardState,
        exercise: EndgameExercise? = null,
        lowMemoryMode: Boolean = false,
        searchDepth: Int = if (lowMemoryMode) 2 else 3,
    ): EndgameCoachReport {
        val engine = ChessEngine(lowMemoryMode = lowMemoryMode)
        val legalBefore = MoveGenerator.legalMoves(stateBefore)
        val playedSan = MoveGenerator.toSan(stateBefore, movePlayed, legalBefore)
        val mover = stateBefore.sideToMove

        val bestLineBefore = engine.evaluateBest(stateBefore, depth = searchDepth, timeLimitMs = 650L)
        val evalBeforeWhiteCp = lineToWhiteCp(mover, bestLineBefore)
        val bestMoveBefore = bestLineBefore?.pvMoves?.firstOrNull()
        val bestSanBefore = bestLineBefore?.pvSan?.firstOrNull()
            ?: bestMoveBefore?.let { MoveGenerator.toSan(stateBefore, it, legalBefore) }
            ?: playedSan

        val (statusAfter, _) = MoveGenerator.evaluateGameResult(stateAfter)
        val bestLineAfter = if (statusAfter.isTerminal) null else {
            engine.evaluateBest(stateAfter, depth = searchDepth, timeLimitMs = 650L)
        }
        val evalAfterWhiteCp = when (statusAfter) {
            GameStatus.CHECKMATE -> if (mover == PieceColor.WHITE) 50_000 else -50_000
            GameStatus.STALEMATE,
            GameStatus.INSUFFICIENT_MATERIAL,
            GameStatus.FIFTY_MOVE_RULE,
            GameStatus.SEVENTY_FIVE_MOVE_RULE,
            GameStatus.THREEFOLD_REPETITION,
            GameStatus.FIVEFOLD_REPETITION,
            -> 0
            else -> lineToWhiteCp(stateAfter.sideToMove, bestLineAfter)
        }

        val evalBeforeMover = if (mover == PieceColor.WHITE) evalBeforeWhiteCp else -evalBeforeWhiteCp
        val evalAfterMover = if (mover == PieceColor.WHITE) evalAfterWhiteCp else -evalAfterWhiteCp
        val cpLoss = (evalBeforeMover - evalAfterMover).coerceAtLeast(0)

        val matchedEngineBest = bestMoveBefore?.uci == movePlayed.uci ||
            (exercise != null && stateBefore.toFen() == exercise.fen && movePlayed.uci == exercise.bestStartUci)

        val quality = when {
            statusAfter == GameStatus.CHECKMATE -> MoveQuality.BEST
            statusAfter == GameStatus.STALEMATE && evalBeforeMover >= 150 -> MoveQuality.BLUNDER
            matchedEngineBest || cpLoss <= 15 -> MoveQuality.BEST
            cpLoss <= 45 -> MoveQuality.EXCELLENT
            cpLoss <= 90 -> MoveQuality.GOOD
            cpLoss <= 180 -> MoveQuality.INACCURACY
            cpLoss <= 340 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }

        val isMistake = quality == MoveQuality.INACCURACY ||
            quality == MoveQuality.MISTAKE ||
            quality == MoveQuality.BLUNDER

        val mistakeExplanation = buildMistakeExplanation(
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            movePlayed = movePlayed,
            playedSan = playedSan,
            bestSan = bestSanBefore,
            evalBeforeWhiteCp = evalBeforeWhiteCp,
            evalAfterWhiteCp = evalAfterWhiteCp,
            evalBeforeMover = evalBeforeMover,
            evalAfterMover = evalAfterMover,
            cpLoss = cpLoss,
            quality = quality,
            statusAfter = statusAfter,
        )

        val classification = EndgameClassifier.classify(stateAfter)
        val classTitle = classification?.let { "${it.category} — ${it.detail}" }
            ?: (exercise?.category ?: "Technical Endgame")

        val nextBestMove = bestLineAfter?.pvMoves?.firstOrNull()
        val nextBestSan = bestLineAfter?.pvSan?.firstOrNull()
            ?: nextBestMove?.let { MoveGenerator.toSan(stateAfter, it, MoveGenerator.legalMoves(stateAfter)) }

        return EndgameCoachReport(
            classificationTitle = classTitle,
            evalFormatted = formatWhiteCp(evalAfterWhiteCp, bestLineAfter?.mateIn, stateAfter.sideToMove),
            evalWhiteCp = evalAfterWhiteCp,
            moveVerdict = EndgameMoveVerdict(
                playedSan = playedSan,
                bestSan = bestSanBefore,
                quality = quality,
                cpLoss = cpLoss,
                isMistake = isMistake,
            ),
            mistakeExplanation = mistakeExplanation,
            weakPoints = detectWeakPoints(stateAfter, classification),
            advantageSummary = buildAdvantageSummary(stateAfter, evalAfterWhiteCp, classification),
            tips = buildTips(stateAfter, classification, exercise, if (isMistake) bestSanBefore else nextBestSan),
            bestMoveUci = if (isMistake) bestMoveBefore?.uci else nextBestMove?.uci,
            bestMoveSan = if (isMistake) bestSanBefore else nextBestSan,
        )
    }

    private fun buildMistakeExplanation(
        stateBefore: ChessBoardState,
        stateAfter: ChessBoardState,
        movePlayed: Move,
        playedSan: String,
        bestSan: String,
        evalBeforeWhiteCp: Int,
        evalAfterWhiteCp: Int,
        evalBeforeMover: Int,
        evalAfterMover: Int,
        cpLoss: Int,
        quality: MoveQuality,
        statusAfter: GameStatus,
    ): String {
        if (statusAfter == GameStatus.CHECKMATE) {
            return "$playedSan delivers checkmate! Clean finishing technique."
        }
        if (statusAfter == GameStatus.STALEMATE && evalBeforeMover >= 150) {
            return "Stalemate blunder ($playedSan): You left the defending king with no legal moves while holding a winning advantage. Always leave at least one escape square or deliver check ($bestSan)."
        }

        val movedPiece = stateBefore.pieceAt(movePlayed.from)
        val mover = stateBefore.sideToMove
        val ownKing = MoveGenerator.findKingSquare(stateBefore, mover)
        val enemyKing = MoveGenerator.findKingSquare(stateAfter, mover.opposite)

        // Check Pawn Endgame premature push (pawn pushed in front of or level with king)
        val isPawnOnly = stateBefore.board.count { it != null && it.type != PieceType.KING && it.type != PieceType.PAWN } == 0
        if (isPawnOnly && movedPiece?.type == PieceType.PAWN && quality != MoveQuality.BEST && cpLoss >= 35) {
            val kingBehindPawn = ownKing != null && (
                (mover == PieceColor.WHITE && ownKing.rank <= movePlayed.to.rank) ||
                    (mover == PieceColor.BLACK && ownKing.rank >= movePlayed.to.rank)
                )
            if (kingBehindPawn && bestSan != playedSan) {
                return "Premature pawn push ($playedSan): Pushing the pawn before your king advances forfeits key squares and lets the defending king blockade. Lead with your king ($bestSan) instead."
            }
        }

        // Check dropped winning advantage into draw
        if (evalBeforeMover >= 180 && abs(evalAfterMover) <= 50 && quality != MoveQuality.BEST) {
            return "Mistake ($playedSan): Gave away a winning advantage (${formatCpSimple(evalBeforeMover)} -> ${formatCpSimple(evalAfterMover)}). $bestSan kept control of key squares and winning chances."
        }

        // Check opposition surrendered in K vs K+P
        if (isPawnOnly && movedPiece?.type == PieceType.KING && ownKing != null && enemyKing != null && cpLoss >= 45) {
            val newKingSq = movePlayed.to
            val lostOpp = abs(newKingSq.file - enemyKing.file) != 0 && abs(newKingSq.rank - enemyKing.rank) != 0
            if (lostOpp && bestSan != playedSan) {
                return "Positional mistake ($playedSan): Stepped away from direct opposition or key outflanking squares. Playing $bestSan keeps the enemy king pushed back."
            }
        }

        if (quality == MoveQuality.BEST || quality == MoveQuality.EXCELLENT || quality == MoveQuality.GOOD) {
            return if (playedSan == bestSan) {
                "$playedSan is the top endgame move — maximizes piece activity and preserves your goal."
            } else {
                "$playedSan is accurate (within ${String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)} pawns of $bestSan) and maintains solid endgame coordination."
            }
        }

        val tacticalBreakdown = MistakeAnalyzer.analyze(
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            movePlayed = movePlayed,
            playedSan = playedSan,
            bestMoveSan = bestSan,
            evalBeforeWhiteCp = evalBeforeWhiteCp,
            evalAfterWhiteCp = evalAfterWhiteCp,
            cpLoss = cpLoss,
            quality = quality,
        )
        return if (tacticalBreakdown != null) {
            "${quality.label} ($playedSan): ${tacticalBreakdown.tacticalReason}. ${tacticalBreakdown.suggestedImprovement}"
        } else {
            "${quality.label} ($playedSan, -${String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)} pawns): Consider $bestSan to keep king and piece coordination sharp."
        }
    }

    fun detectWeakPoints(
        state: ChessBoardState,
        classification: EndgameClassification? = EndgameClassifier.classify(state),
    ): List<String> {
        val points = mutableListOf<String>()

        for (color in listOf(PieceColor.WHITE, PieceColor.BLACK)) {
            val label = if (color == PieceColor.WHITE) "White" else "Black"
            val kingSq = MoveGenerator.findKingSquare(state, color)
            if (kingSq != null) {
                val centerDist = kingCenterDistance(kingSq)
                val edgeRank = if (color == PieceColor.WHITE) 0 else 7
                if (kingSq.rank == edgeRank && centerDist >= 3) {
                    points.add("$label's king is passive on ${kingSq.algebraic} — bring the king toward the center and pawn action.")
                }
            }

            // Pawn structure weaknesses: isolated, doubled, hanging
            val pawnFiles = IntArray(8)
            val pawnSquares = mutableListOf<Square>()
            for (i in 0..63) {
                val p = state.board[i] ?: continue
                if (p.color == color && p.type == PieceType.PAWN) {
                    val sq = Square(i)
                    pawnFiles[sq.file]++
                    pawnSquares.add(sq)
                }
            }

            val isolated = pawnSquares.filter { sq ->
                val leftHas = sq.file > 0 && pawnFiles[sq.file - 1] > 0
                val rightHas = sq.file < 7 && pawnFiles[sq.file + 1] > 0
                !leftHas && !rightHas
            }
            if (isolated.isNotEmpty() && pawnSquares.size >= 2) {
                points.add("$label has isolated pawn(s) on ${isolated.joinToString { it.algebraic }} with no neighbor pawn support.")
            }

            val doubledFiles = (0..7).filter { pawnFiles[it] >= 2 }.map { ('a' + it).toString() }
            if (doubledFiles.isNotEmpty()) {
                points.add("$label has doubled pawns on the ${doubledFiles.joinToString()}-file, creating static targets.")
            }

            val undefendedPawns = pawnSquares.filter { sq ->
                MoveGenerator.isSquareAttacked(state, sq, color.opposite) &&
                    !MoveGenerator.isSquareAttacked(state, sq, color)
            }
            if (undefendedPawns.isNotEmpty()) {
                points.add("$label's pawn on ${undefendedPawns.first().algebraic} is attacked and undefended.")
            }

            // Rook placed in front of own passed pawn (blocks its advance)
            for (i in 0..63) {
                val p = state.board[i] ?: continue
                if (p.color != color) continue
                val sq = Square(i)
                if (p.type == PieceType.ROOK) {
                    val blockingPassedPawn = pawnSquares.any { ps ->
                        ps.file == sq.file && isPassedPawn(state, ps, color) &&
                            ((color == PieceColor.WHITE && sq.rank > ps.rank) ||
                                (color == PieceColor.BLACK && sq.rank < ps.rank))
                    }
                    if (blockingPassedPawn) {
                        points.add("$label's rook on ${sq.algebraic} stands in front of its own passed pawn instead of supporting from behind.")
                    }
                } else if (p.type == PieceType.BISHOP && pawnSquares.size >= 2) {
                    val sameColorPawns = pawnSquares.count { it.isLight == sq.isLight }
                    if (sameColorPawns >= 2 && sameColorPawns * 2 > pawnSquares.size) {
                        points.add("$label's bishop on ${sq.algebraic} is hemmed in by pawns on the same square color.")
                    }
                }
            }
        }

        // Stalemate danger when one side is far ahead and defender has <= 2 legal moves
        val matDiff = materialDiffWhite(state)
        val defenderLegal = MoveGenerator.legalMoves(state)
        if (abs(matDiff) >= 500 && defenderLegal.size in 1..2 && !MoveGenerator.isKingInCheck(state, state.sideToMove)) {
            points.add("High stalemate risk: ${if (state.sideToMove == PieceColor.WHITE) "White" else "Black"} only has ${defenderLegal.size} legal move(s) remaining.")
        }

        if (points.isEmpty()) {
            val fallback = when {
                classification?.isTheoreticalDraw == true -> "Neither side has a structural target — precision is needed to hold or test the draw."
                else -> "Key squares ahead of the pawns and king activity will decide who breaks through."
            }
            points.add(fallback)
        }
        return points.take(3)
    }

    fun buildAdvantageSummary(
        state: ChessBoardState,
        evalWhiteCp: Int,
        classification: EndgameClassification? = EndgameClassifier.classify(state),
    ): String {
        val whitePassed = findPassedPawns(state, PieceColor.WHITE)
        val blackPassed = findPassedPawns(state, PieceColor.BLACK)
        val wKing = MoveGenerator.findKingSquare(state, PieceColor.WHITE)
        val bKing = MoveGenerator.findKingSquare(state, PieceColor.BLACK)
        val matDiff = materialDiffWhite(state)

        val reasons = mutableListOf<String>()
        if (abs(matDiff) >= 100) {
            val leader = if (matDiff > 0) "White" else "Black"
            reasons.add("$leader leads by ${String.format(java.util.Locale.US, "%.1f", abs(matDiff) / 100.0)} pawns of material")
        }
        if (whitePassed.isNotEmpty()) {
            reasons.add("White has passed pawn(s) on ${whitePassed.joinToString { it.algebraic }}")
        }
        if (blackPassed.isNotEmpty()) {
            reasons.add("Black has passed pawn(s) on ${blackPassed.joinToString { it.algebraic }}")
        }
        if (wKing != null && bKing != null) {
            val wDist = kingCenterDistance(wKing)
            val bDist = kingCenterDistance(bKing)
            if (wDist + 1 <= bDist) reasons.add("White's king (${wKing.algebraic}) is more centralized")
            else if (bDist + 1 <= wDist) reasons.add("Black's king (${bKing.algebraic}) is more active")
        }

        val verdict = when {
            classification?.isTheoreticalDraw == true && abs(evalWhiteCp) < 120 ->
                "Theoretical Draw Zone (${formatCpSimple(evalWhiteCp)})"
            evalWhiteCp >= 200 -> "White holds a winning advantage (${formatCpSimple(evalWhiteCp)})"
            evalWhiteCp >= 65 -> "White is clearly better (${formatCpSimple(evalWhiteCp)})"
            evalWhiteCp <= -200 -> "Black holds a winning advantage (${formatCpSimple(evalWhiteCp)})"
            evalWhiteCp <= -65 -> "Black is clearly better (${formatCpSimple(evalWhiteCp)})"
            else -> "Balanced / Equal endgame (${formatCpSimple(evalWhiteCp)})"
        }

        val detail = if (reasons.isNotEmpty()) reasons.joinToString("; ") + "." else "Piece coordination and tempo govern the balance."
        return "$verdict — $detail"
    }

    fun buildTips(
        state: ChessBoardState,
        classification: EndgameClassification?,
        exercise: EndgameExercise?,
        bestSan: String?,
    ): List<String> {
        val tips = mutableListOf<String>()
        exercise?.tip?.takeIf { it.isNotBlank() }?.let { tips.add(it) }
        bestSan?.let { tips.add("Candidate focus: calculate $it and check your opponent's most active reply.") }

        val cat = classification?.category.orEmpty()
        when {
            cat.contains("Pawn Endgame", ignoreCase = true) -> {
                tips.add("Rule of Opposition & Key Squares: keep your king ahead of the pawn so the enemy king cannot blockade.")
            }
            cat.contains("Rook", ignoreCase = true) -> {
                tips.add("Tarrasch Rule: place rooks behind passed pawns (yours or your opponent's) and cut off the enemy king by a file.")
            }
            cat.contains("Queen", ignoreCase = true) -> {
                tips.add("Always verify stalemate before moving; bring your own king into the net to deliver checkmate.")
            }
            cat.contains("Bishop", ignoreCase = true) -> {
                tips.add("Keep pawns on the opposite square color of your bishop and fight across both flanks.")
            }
            cat.contains("Knight", ignoreCase = true) -> {
                tips.add("Centralize your knight — knights lose half their targets on the edge of the board.")
            }
            else -> {
                tips.add("Activate your king as an attacking piece once queens leave the board.")
            }
        }
        return tips.distinct().take(3)
    }

    private fun isPassedPawn(state: ChessBoardState, sq: Square, color: PieceColor): Boolean {
        val enemy = color.opposite
        for (f in (sq.file - 1).coerceAtLeast(0)..(sq.file + 1).coerceAtMost(7)) {
            val rankRange = if (color == PieceColor.WHITE) (sq.rank + 1)..7 else 0 until sq.rank
            for (r in rankRange) {
                val p = state.pieceAt(Square.of(f, r))
                if (p?.color == enemy && p.type == PieceType.PAWN) return false
            }
        }
        return true
    }

    private fun findPassedPawns(state: ChessBoardState, color: PieceColor): List<Square> {
        val res = mutableListOf<Square>()
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.color == color && p.type == PieceType.PAWN) {
                val sq = Square(i)
                if (isPassedPawn(state, sq, color)) res.add(sq)
            }
        }
        return res
    }

    private fun kingCenterDistance(sq: Square): Int {
        val df = minOf(abs(sq.file - 3), abs(sq.file - 4))
        val dr = minOf(abs(sq.rank - 3), abs(sq.rank - 4))
        return maxOf(df, dr)
    }

    private fun materialDiffWhite(state: ChessBoardState): Int {
        var diff = 0
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.type == PieceType.KING) continue
            diff += if (p.color == PieceColor.WHITE) p.type.materialValue else -p.type.materialValue
        }
        return diff
    }

    private fun lineToWhiteCp(sideToMove: PieceColor, line: EngineLine?): Int {
        if (line == null) return 0
        return line.scoreCp
    }

    private fun formatWhiteCp(evalWhiteCp: Int, mateIn: Int?, @Suppress("UNUSED_PARAMETER") sideToMove: PieceColor): String {
        if (mateIn != null) return if (mateIn > 0) "+M$mateIn" else "-M${abs(mateIn)}"
        return formatCpSimple(evalWhiteCp)
    }

    private fun formatCpSimple(cp: Int): String {
        if (abs(cp) >= 45_000) return if (cp > 0) "+M" else "-M"
        val pawns = cp / 100.0
        return if (pawns >= 0) String.format(java.util.Locale.US, "+%.2f", pawns)
        else String.format(java.util.Locale.US, "%.2f", pawns)
    }
}
