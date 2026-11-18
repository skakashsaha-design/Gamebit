package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundAndHapticsManager
import com.example.chess.ChessVariant
import com.example.chess.Move
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.chess.TimeControl
import com.example.data.local.ChessHubDatabase
import com.example.data.local.LocalChessRepository
import com.example.viewmodel.LocalGameViewModel
import com.example.viewmodel.PlayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalGameHistoryAndPgnTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: ChessHubDatabase
    private lateinit var vm: LocalGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ChessHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalChessRepository(
            dao = db.chessHubDao(),
            puzzleDao = db.chessPuzzleDao(),
            mediaDao = db.mediaAndNewsDao(),
        )
        vm = LocalGameViewModel(
            localRepo = repo,
            soundManager = SoundAndHapticsManager(context),
        )
        vm.startNewGame(
            mode = PlayMode.PASS_AND_PLAY,
            variant = ChessVariant.STANDARD,
            timeControl = TimeControl.PRESETS[5],
            humanColor = PieceColor.WHITE,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `move history supports stepping back and forth through moves and returning to live`() {
        val e2e4 = Move(Square.fromAlgebraic("e2")!!, Square.fromAlgebraic("e4")!!)
        val e7e5 = Move(Square.fromAlgebraic("e7")!!, Square.fromAlgebraic("e5")!!)
        val g1f3 = Move(Square.fromAlgebraic("g1")!!, Square.fromAlgebraic("f3")!!)

        vm.onUserMove(e2e4)
        vm.onUserMove(e7e5)
        vm.onUserMove(g1f3)

        val liveState = vm.uiState.value
        assertEquals(listOf("e4", "e5", "Nf3"), liveState.moveHistorySan)
        assertEquals(3, liveState.viewingPly)
        assertTrue(liveState.isViewingLive)
        assertNotNull(liveState.displayedBoardState.pieceAt(Square.fromAlgebraic("f3")!!))

        // Step backward 2 moves -> viewing e4 (ply 1)
        vm.stepPreviousMove()
        assertEquals(2, vm.uiState.value.viewingPly)
        assertFalse(vm.uiState.value.isViewingLive)
        assertNull(vm.uiState.value.displayedBoardState.pieceAt(Square.fromAlgebraic("f3")!!))

        vm.stepPreviousMove()
        assertEquals(1, vm.uiState.value.viewingPly)
        assertEquals("e2e4", vm.uiState.value.displayedLastMove?.uci)
        assertNull(vm.uiState.value.displayedBoardState.pieceAt(Square.fromAlgebraic("e5")!!))

        // Jump to initial position (ply 0)
        vm.stepFirstMove()
        assertEquals(0, vm.uiState.value.viewingPly)
        assertNull(vm.uiState.value.displayedLastMove)
        assertNotNull(vm.uiState.value.displayedBoardState.pieceAt(Square.fromAlgebraic("e2")!!))

        // Step forward to ply 2 and jump to live ply 3
        vm.jumpToPly(2)
        assertEquals(2, vm.uiState.value.viewingPly)
        assertEquals("e7e5", vm.uiState.value.displayedLastMove?.uci)

        vm.stepLatestMove()
        assertTrue(vm.uiState.value.isViewingLive)
        assertEquals(3, vm.uiState.value.viewingPly)
    }

    @Test
    fun `pgn import and export populates move history and supports navigation`() {
        val samplePgn = """
            [Event "Test Import"]
            [White "White Player"]
            [Black "Black Player"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6
        """.trimIndent()

        val loaded = vm.importPgnIntoLocalGame(samplePgn)
        assertTrue(loaded)

        val st = vm.uiState.value
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6"), st.moveHistorySan)
        assertEquals(6, st.viewingPly)
        assertTrue(st.isViewingLive)

        vm.stepPreviousMove()
        assertEquals(5, vm.uiState.value.viewingPly)
        assertEquals("f1b5", vm.uiState.value.displayedLastMove?.uci)

        val exported = vm.exportCurrentPgn()
        assertTrue(exported.contains("1. e4"))
        assertTrue(exported.contains("3. Bb5 a6"))

        // Invalid PGN text returns false safely
        assertFalse(vm.importPgnIntoLocalGame("1. Zz9"))
    }
}
