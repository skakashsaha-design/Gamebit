package com.example

import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessVariant
import com.example.chess.EndgameClassifier
import com.example.chess.GameStatus
import com.example.chess.Glicko2RatingSystem
import com.example.chess.GlickoRating
import com.example.chess.MistakeAnalyzer
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveQuality
import com.example.chess.OpeningBook
import com.example.chess.PgnParser
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.chess.UciChessEngine
import com.example.data.local.PuzzlesAndLessonsCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesAndEngineTest {

    @Test
    fun `standard starting position has 20 legal moves`() {
        val state = ChessBoardState.standard()
        val moves = MoveGenerator.legalMoves(state)
        assertEquals(20, moves.size)
    }

    @Test
    fun `castling and en passant and promotion work legally`() {
        val epState = ChessBoardState.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").getOrThrow()
        val epMoves = MoveGenerator.legalMoves(epState)
        assertTrue(epMoves.any { it.from == Square.fromAlgebraic("e5") && it.to == Square.fromAlgebraic("d6") && it.isEnPassant })

        val promoState = ChessBoardState.fromFen("8/4P1k1/8/8/8/8/8/4K3 w - - 0 1").getOrThrow()
        val promoMoves = MoveGenerator.legalMoves(promoState).filter {
            it.from == Square.fromAlgebraic("e7") && it.to == Square.fromAlgebraic("e8")
        }
        assertEquals(4, promoMoves.size)
        assertTrue(promoMoves.any { it.promotion == PieceType.KNIGHT })
    }

    @Test
    fun `fools mate checkmate and stalemate detection`() {
        var st = ChessBoardState.standard()
        listOf("f2f3", "e7e5", "g2g4", "d8h4").forEach { uci ->
            val m = MoveGenerator.legalMoves(st).first { it.uci == uci }
            st = MoveGenerator.applyMoveUnchecked(st, m)
        }
        val (status, winner) = MoveGenerator.evaluateGameResult(st)
        assertEquals(GameStatus.CHECKMATE, status)
        assertEquals(PieceColor.BLACK, winner)

        val stalemateState = ChessBoardState.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1").getOrThrow()
        val (stStatus, stWinner) = MoveGenerator.evaluateGameResult(stalemateState)
        assertEquals(GameStatus.STALEMATE, stStatus)
        assertEquals(null, stWinner)
    }

    @Test
    fun `chess960 generates valid back-rank positions for indices 0 to 959`() {
        for (id in listOf(0, 128, 518, 777, 959)) {
            val st = ChessBoardState.chess960(id)
            assertEquals(ChessVariant.CHESS960, st.variant)
            val backRank = (0..7).map { st.pieceAt(it, 0)!! }
            assertEquals(1, backRank.count { it.type == PieceType.KING })
            assertEquals(2, backRank.count { it.type == PieceType.ROOK })
        }
    }

    @Test
    fun `all 9 variants initialize and produce legal moves`() {
        ChessVariant.entries.forEach { variant ->
            val state = ChessBoardState.initialForVariant(variant)
            val legal = MoveGenerator.legalMoves(state)
            assertTrue("Expected legal moves for ${variant.name}", legal.isNotEmpty())
        }
    }

    @Test
    fun `three check and koth and antichess variant wins`() {
        val koth = ChessBoardState.fromFen("4k3/8/8/8/4K3/8/8/8 b - - 0 1", ChessVariant.KING_OF_THE_HILL).getOrThrow()
        val (kStatus, kWin) = MoveGenerator.evaluateGameResult(koth)
        assertEquals(GameStatus.VARIANT_WIN, kStatus)
        assertEquals(PieceColor.WHITE, kWin)

        val anti = ChessBoardState.fromFen("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", ChessVariant.ANTICHESS).getOrThrow()
        val antiMoves = MoveGenerator.legalMoves(anti)
        assertTrue(antiMoves.all { anti.pieceAt(it.to) != null })
    }

    @Test
    fun `pgn import export preserves moves comments and variations`() {
        val samplePgn = """
            [Event "Test Game"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]

            1. e4 {Best by test} e5 (1... c5 {Sicilian}) 2. Nf3 Nc6 3. Bb5 1-0
        """.trimIndent()
        val parsed = PgnParser.parseMultiGamePgn(samplePgn).getOrThrow()
        assertEquals(1, parsed.size)
        val main = parsed.first().rootNode.mainLine()
        assertEquals(5, main.size)
        assertEquals("Best by test", main[0].comment)
        assertEquals(2, main[0].children.size)

        val exported = PgnParser.exportPgn(parsed.first().rootNode, parsed.first().headers, "1-0")
        assertTrue(exported.contains("1. e4"))
        assertTrue(exported.contains("Bb5"))
    }

    @Test
    fun `opening recognition all 12 gambits and glicko2 calculation`() {
        val match = OpeningBook.recognize(listOf("e2e4", "c7c5", "g1f3"))
        assertNotNull(match)
        assertTrue(match!!.eco.startsWith("B"))

        assertEquals(12, OpeningBook.NAMED_GAMBITS.size)
        OpeningBook.NAMED_GAMBITS.forEach { gambit ->
            val accepted = OpeningBook.recognizeGambit(gambit.acceptedLineUci)
            assertNotNull("Gambit ${gambit.name} accepted line must be recognized", accepted)
            assertEquals(gambit.name, accepted!!.name)
        }

        val before = GlickoRating(1500.0, 200.0, 0.06)
        val afterWin = Glicko2RatingSystem.updateSingleGame(before, GlickoRating(1500.0, 150.0, 0.06), 1.0)
        assertTrue(afterWin.rating > before.rating)
        assertTrue(afterWin.rd < before.rd)
    }

    @Test
    fun `all offline puzzles lessons drills endgames and 20 checkmate patterns are valid`() {
        PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.forEach { puzzle ->
            var state = ChessBoardState.fromFen(puzzle.fen).getOrThrow()
            assertFalse("Puzzle ${puzzle.id} solution should not be empty", puzzle.solutionUci.isEmpty())
            puzzle.solutionUci.forEach { uci ->
                val legal = MoveGenerator.legalMoves(state)
                val move = legal.firstOrNull { it.uci == uci }
                assertNotNull("Illegal puzzle move $uci in ${puzzle.id} (${puzzle.fen})", move)
                state = MoveGenerator.applyMoveUnchecked(state, move!!)
            }
            if (puzzle.isCheckmatePattern) {
                val (status, _) = MoveGenerator.evaluateGameResult(state)
                assertEquals(
                    "Checkmate pattern ${puzzle.patternName} (${puzzle.id}) must end in CHECKMATE",
                    GameStatus.CHECKMATE,
                    status,
                )
            }
        }

        // Verify all 20 named checkmate patterns exist
        PuzzlesAndLessonsCatalog.CHECKMATE_PATTERN_NAMES.forEach { patternName ->
            assertTrue(
                "Missing checkmate pattern: $patternName",
                PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.any { it.isCheckmatePattern && it.patternName == patternName },
            )
        }

        PuzzlesAndLessonsCatalog.OPENING_DRILLS.forEach { drill ->
            var st = ChessBoardState.standard()
            drill.lineMovesUci.forEach { uci ->
                val m = MoveGenerator.legalMoves(st).firstOrNull { it.uci == uci }
                assertNotNull("Illegal drill setup move $uci in ${drill.id}", m)
                st = MoveGenerator.applyMoveUnchecked(st, m!!)
            }
            assertTrue(
                "Drill ${drill.id} expected move ${drill.expectedMoveUci} must be legal",
                MoveGenerator.legalMoves(st).any { it.uci == drill.expectedMoveUci },
            )
        }

        PuzzlesAndLessonsCatalog.LESSONS.forEach { lesson ->
            lesson.steps.forEach { step ->
                val st = ChessBoardState.fromFen(step.fen).getOrThrow()
                step.expectedMoveUci?.let { uci ->
                    assertTrue(
                        "Lesson ${lesson.id} step ${step.title} move $uci must be legal",
                        MoveGenerator.legalMoves(st).any { it.uci == uci },
                    )
                }
            }
        }

        PuzzlesAndLessonsCatalog.ENDGAMES.forEach { eg ->
            val st = ChessBoardState.fromFen(eg.fen).getOrThrow()
            assertTrue(
                "Endgame ${eg.id} start move ${eg.bestStartUci} must be legal",
                MoveGenerator.legalMoves(st).any { it.uci == eg.bestStartUci },
            )
        }
    }

    @Test
    fun `endgame classifier and mistake analyzer produce accurate breakdowns`() {
        val bareKings = ChessBoardState.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").getOrThrow()
        val bareClass = EndgameClassifier.classify(bareKings)
        assertNotNull(bareClass)
        assertTrue(bareClass!!.isTheoreticalDraw)

        val rookEndgame = ChessBoardState.fromFen("4k3/5p2/8/8/8/8/5P2/4K2R w - - 0 1").getOrThrow()
        assertEquals("Rook Endgame", EndgameClassifier.classify(rookEndgame)?.category)

        // Blunder check: 1. f3 e5 2. g4?? allowing Qh4#
        var st = ChessBoardState.standard()
        st = MoveGenerator.applyMoveUnchecked(st, Move(Square.fromAlgebraic("f2")!!, Square.fromAlgebraic("f3")!!))
        st = MoveGenerator.applyMoveUnchecked(st, Move(Square.fromAlgebraic("e7")!!, Square.fromAlgebraic("e5")!!))
        val blunderMove = Move(Square.fromAlgebraic("g2")!!, Square.fromAlgebraic("g4")!!)
        val afterBlunder = MoveGenerator.applyMoveUnchecked(st, blunderMove)
        val breakdown = MistakeAnalyzer.analyze(
            stateBefore = st,
            stateAfter = afterBlunder,
            movePlayed = blunderMove,
            playedSan = "g4",
            bestMoveSan = "e4",
            evalBeforeWhiteCp = -30,
            evalAfterWhiteCp = -50000,
            cpLoss = 5000,
            quality = MoveQuality.BLUNDER,
        )
        assertNotNull(breakdown)
        assertTrue(breakdown!!.tacticalReason.contains("mate", ignoreCase = true))
        assertEquals("e4", breakdown.bestMoveSan)
    }

    @Test
    fun `engine finds mate in 1, uci console responds, and game review succeeds`() = runTest {
        val engine = ChessEngine(lowMemoryMode = true)
        val mateIn1 = ChessBoardState.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1").getOrThrow()
        val best = engine.evaluateBest(mateIn1, depth = 2, timeLimitMs = 800L)
        assertNotNull(best)
        assertEquals("d1d8", best!!.pvMoves.first().uci)

        val uciEngine = UciChessEngine(lowMemoryMode = true)
        val uciOut = uciEngine.sendCommand("uci")
        assertTrue(uciOut.any { it.contains("ChessHub Kotlin UCI Engine") })
        assertTrue(uciOut.contains("uciok"))
        assertEquals(listOf("readyok"), uciEngine.sendCommand("isready"))
        uciEngine.sendCommand("position startpos moves e2e4 e7e5")
        val goOut = uciEngine.sendCommand("go depth 1")
        assertTrue(goOut.any { it.startsWith("bestmove ") })
        uciEngine.sendCommand("stop")

        val moves = listOf("e2e4", "e7e5", "g1f3", "b8c6").map { uci ->
            Move(Square.fromAlgebraic(uci.take(2))!!, Square.fromAlgebraic(uci.drop(2))!!)
        }
        val report = engine.reviewGame(ChessBoardState.standard(), moves)
        assertEquals(4, report.moves.size)
        assertTrue(report.whiteAccuracy in 0..100)
    }
}
