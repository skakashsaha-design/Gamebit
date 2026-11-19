package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.chess.ChessBoardState
import com.example.chess.EndgameCoach
import com.example.chess.MoveGenerator
import com.example.chess.MoveQuality
import com.example.data.local.ChessHubDatabase
import com.example.data.local.LocalChessRepository
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.data.local.SettingsRepository
import com.example.data.remote.ExternalChessApis
import com.example.viewmodel.TrainingViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class EndgameCoachUnitTest {

    @Test
    fun `detectWeakPoints identifies passive edge king, isolated pawns, and rook blocking passed pawn`() {
        val rookBlockState = ChessBoardState.fromFen("4k3/4R3/8/4P3/8/8/8/4K3 w - - 0 1").getOrThrow()
        val rookBlockWeak = EndgameCoach.detectWeakPoints(rookBlockState)
        assertTrue(
            "Expected rook blocking passed pawn weak point, got $rookBlockWeak",
            rookBlockWeak.any { it.contains("rook", ignoreCase = true) && it.contains("passed pawn", ignoreCase = true) },
        )

        val isolatedState = ChessBoardState.fromFen("4k3/8/8/8/P4P2/8/8/4K3 w - - 0 1").getOrThrow()
        val isolatedWeak = EndgameCoach.detectWeakPoints(isolatedState)
        assertTrue(
            "Expected isolated pawn weak point, got $isolatedWeak",
            isolatedWeak.any { it.contains("isolated", ignoreCase = true) },
        )

        val edgeKingState = ChessBoardState.fromFen("7k/8/8/8/4P3/8/8/K7 w - - 0 1").getOrThrow()
        val edgeKingWeak = EndgameCoach.detectWeakPoints(edgeKingState)
        assertTrue(
            "Expected passive king weak point, got $edgeKingWeak",
            edgeKingWeak.any { it.contains("king is passive", ignoreCase = true) },
        )
    }

    @Test
    fun `analyzeMove catches stalemate blunder and returns full coaching breakdown`() = runTest {
        val stateBefore = ChessBoardState.fromFen("7k/8/5Q1K/8/8/8/8/8 w - - 0 1").getOrThrow()
        val stalemateMove = MoveGenerator.legalMoves(stateBefore).first { it.uci == "f6g6" }
        val stateAfter = MoveGenerator.applyMoveUnchecked(stateBefore, stalemateMove)

        val report = EndgameCoach.analyzeMove(
            stateBefore = stateBefore,
            movePlayed = stalemateMove,
            stateAfter = stateAfter,
            exercise = PuzzlesAndLessonsCatalog.ENDGAMES.first { it.id == "eg_queen_1" },
            lowMemoryMode = true,
            searchDepth = 2,
        )

        assertNotNull(report.moveVerdict)
        assertEquals(MoveQuality.BLUNDER, report.moveVerdict!!.quality)
        assertTrue(report.moveVerdict!!.isMistake)
        assertTrue(
            "Expected stalemate explanation, got: ${report.mistakeExplanation}",
            report.mistakeExplanation.contains("stalemate", ignoreCase = true),
        )
        assertTrue(report.weakPoints.isNotEmpty())
        assertTrue(report.advantageSummary.isNotBlank())
        assertTrue(report.tips.isNotEmpty())
    }

    @Test
    fun `analyzePosition produces weak points, advantage, and tips for all catalog endgames`() = runTest {
        assertTrue(PuzzlesAndLessonsCatalog.ENDGAMES.size >= 8)
        PuzzlesAndLessonsCatalog.ENDGAMES.forEach { ex ->
            val st = ChessBoardState.fromFen(ex.fen).getOrThrow()
            val legal = MoveGenerator.legalMoves(st)
            assertTrue("Expected legal bestStartUci ${ex.bestStartUci} for ${ex.id}", legal.any { it.uci == ex.bestStartUci })
        }

        val lucena = PuzzlesAndLessonsCatalog.ENDGAMES.first { it.id == "eg_lucena_1" }
        val lucenaState = ChessBoardState.fromFen(lucena.fen).getOrThrow()
        val report = EndgameCoach.analyzePosition(
            state = lucenaState,
            exercise = lucena,
            lowMemoryMode = true,
            searchDepth = 2,
        )
        assertTrue(report.weakPoints.isNotEmpty())
        assertTrue(report.advantageSummary.isNotBlank())
        assertTrue(report.tips.isNotEmpty())
        assertTrue(report.evalFormatted.isNotBlank())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class EndgameTrainerAndAnimationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `endgame trainer screen renders board, coaching toggles, and slide animation toggle`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("continue_offline_button")).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("nav_learn")).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodes(hasTestTag("continue_offline_button")).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("continue_offline_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("nav_learn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("learn_tab_endgames").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chess_board").assertIsDisplayed()
        composeRule.onNodeWithTag("endgame_coach_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("endgame_slide_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("endgame_best_arrow_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("endgame_coach_card").assertIsDisplayed()

        // Wait for initial EndgameCoach analysis to populate Mistake / Weak Points / Advantage / Tips
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Mistake & Move Check")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mistake & Move Check")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Weak Points")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Advantage")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Tips")
            .performScrollTo()
            .assertIsDisplayed()

        // Scroll back to toggle controls and toggle Slide Anim & Coach Explain
        composeRule.onNodeWithTag("endgame_slide_toggle")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("endgame_slide_toggle").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("endgame_coach_toggle")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodes(hasTestTag("endgame_coach_card")).fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithTag("endgame_coach_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("endgame_coach_card")
            .performScrollTo()
            .assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EndgameTrainingViewModelTest {

    @Test
    fun `training viewmodel endgame trainer submits move, updates history, and supports undo and coach toggle`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ChessHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val localRepo = LocalChessRepository(db.chessHubDao(), db.chessPuzzleDao(), db.mediaAndNewsDao())
            val settingsRepo = SettingsRepository(context)
            val vm = TrainingViewModel(localRepo, settingsRepo, ExternalChessApis())

            val kp = PuzzlesAndLessonsCatalog.ENDGAMES.first { it.id == "eg_kp_1" }
            vm.selectEndgameExercise(kp)
            assertEquals("eg_kp_1", vm.endgameState.value.exercise.id)
            assertTrue(vm.endgameState.value.coachEnabled)

            vm.toggleEndgameCoachEnabled(false)
            assertFalse(vm.endgameState.value.coachEnabled)
            vm.toggleEndgameCoachEnabled(true)
            assertTrue(vm.endgameState.value.coachEnabled)

            vm.toggleEndgameBestMoveArrow()
            assertTrue(vm.endgameState.value.showBestMoveArrow)

            val move = vm.endgameState.value.legalMoves.first { it.uci == kp.bestStartUci }
            vm.submitEndgameMove(move)
            assertTrue(vm.endgameState.value.moveHistorySan.isNotEmpty())
            assertTrue(vm.endgameState.value.canUndo)

            vm.undoEndgameMove()
            assertEquals(0, vm.endgameState.value.moveHistorySan.size)
            assertFalse(vm.endgameState.value.canUndo)
        } finally {
            db.close()
        }
    }
}
