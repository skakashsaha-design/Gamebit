package com.example

import com.example.chess.AiDifficulty
import com.example.chess.ChessBoardState
import com.example.chess.GameStatus
import com.example.chess.KotlinChessEngineAdapter
import com.example.chess.LocalChessEngine
import com.example.chess.Move
import com.example.chess.MoveValidationResult
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalChessEngineTest {

    private val engine: LocalChessEngine = KotlinChessEngineAdapter(lowMemoryMode = true)

    @Test
    fun `validateMove and validateUciMove enforce legal chess moves and reject illegal moves`() {
        val start = ChessBoardState.standard()
        assertEquals(20, engine.legalMoves(start).size)

        val validE4 = Move(Square.fromAlgebraic("e2")!!, Square.fromAlgebraic("e4")!!)
        assertTrue(engine.isMoveLegal(start, validE4))

        val res = engine.validateMove(start, validE4)
        assertTrue(res is MoveValidationResult.Valid)
        val valid = res as MoveValidationResult.Valid
        assertEquals("e4", valid.san)
        assertEquals(PieceColor.BLACK, valid.nextState.sideToMove)
        assertEquals(GameStatus.IN_PROGRESS, valid.gameStatus)
        assertFalse(valid.isCheck)

        // Illegal king jump e1 -> e4
        val illegalMove = Move(Square.fromAlgebraic("e1")!!, Square.fromAlgebraic("e4")!!)
        assertFalse(engine.isMoveLegal(start, illegalMove))
        val invalidRes = engine.validateMove(start, illegalMove)
        assertTrue(invalidRes is MoveValidationResult.Invalid)

        // Malformed / illegal UCI string
        assertTrue(engine.validateUciMove(start, "e2e5") is MoveValidationResult.Invalid)
        assertTrue(engine.validateUciMove(start, "bad") is MoveValidationResult.Invalid)

        // Promotion resolution (default queen vs explicit knight underpromotion)
        val promoBoard = ChessBoardState.fromFen("8/4P1k1/8/8/8/8/8/4K3 w - - 0 1").getOrThrow()
        val defaultPromo = Move(Square.fromAlgebraic("e7")!!, Square.fromAlgebraic("e8")!!)
        val defaultPromoRes = engine.validateMove(promoBoard, defaultPromo) as MoveValidationResult.Valid
        assertEquals(PieceType.QUEEN, defaultPromoRes.resolvedMove.promotion)

        val knightPromoRes = engine.validateUciMove(promoBoard, "e7e8n") as MoveValidationResult.Valid
        assertEquals(PieceType.KNIGHT, knightPromoRes.resolvedMove.promotion)
    }

    @Test
    fun `validateUciMove detects checkmate terminal status`() {
        var state = ChessBoardState.standard()
        listOf("f2f3", "e7e5", "g2g4").forEach { uci ->
            val valid = engine.validateUciMove(state, uci) as MoveValidationResult.Valid
            state = valid.nextState
        }
        val mateRes = engine.validateUciMove(state, "d8h4") as MoveValidationResult.Valid
        assertTrue(mateRes.isCheck)
        assertEquals(GameStatus.CHECKMATE, mateRes.gameStatus)
        assertEquals(PieceColor.BLACK, mateRes.winner)

        // Subsequent move after game over is rejected
        val afterMate = engine.validateUciMove(mateRes.nextState, "e2e4")
        assertTrue(afterMate is MoveValidationResult.Invalid)
    }

    @Test
    fun `all AI difficulty levels select legal moves and stronger tiers find mate in 1`() = runTest {
        val start = ChessBoardState.standard()
        val mateIn1 = ChessBoardState.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1").getOrThrow()

        AiDifficulty.entries.forEach { difficulty ->
            val openingMove = engine.chooseMove(start, difficulty, useOpeningBook = false)
            assertNotNull("Difficulty $difficulty must return a legal opening move", openingMove)
            assertTrue(engine.isMoveLegal(start, openingMove!!))

            val bot = difficulty.toBotProfile()
            assertEquals(difficulty, AiDifficulty.fromBotProfile(bot))
        }

        listOf(AiDifficulty.HARD, AiDifficulty.EXPERT, AiDifficulty.MASTER).forEach { strongTier ->
            val tacticalMove = engine.chooseMove(mateIn1, strongTier, useOpeningBook = false)
            assertNotNull(tacticalMove)
            assertEquals("Expected $strongTier to find Rd8#", "d1d8", tacticalMove!!.uci)

            val bestLine = engine.evaluateBestMove(mateIn1, strongTier)
            assertNotNull(bestLine)
            assertEquals("d1d8", bestLine!!.pvMoves.first().uci)
        }
    }
}
