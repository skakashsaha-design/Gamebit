package com.example

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessHubBots
import com.example.chess.ChessVariant
import com.example.chess.EndgameClassifier
import com.example.chess.GameReviewReport
import com.example.chess.GameStatus
import com.example.chess.MoveGenerator
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.ReviewedMove
import com.example.chess.TimeControl
import com.example.data.local.AnimationMode
import com.example.data.local.AppSettings
import com.example.data.local.PerformanceMode
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.data.local.RatingCategoryStats
import com.example.data.local.SettingsRepository
import com.example.data.local.VoiceMode
import com.example.ui.components.BotAvatarView
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationGraph
import com.example.ui.components.PlayerAvatarCatalog
import com.example.ui.components.PlayerAvatarView
import com.example.ui.components.PostGameComparisonDialog
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.ProfileAndLookupTab
import com.example.ui.theme.ChessBoardThemes
import com.example.ui.theme.ChessHubTheme
import com.example.viewmodel.CoordinateTrainerState
import com.example.viewmodel.EndgameTrainerState
import com.example.viewmodel.LocalGameUiState
import com.example.viewmodel.OpeningDrillSessionState
import com.example.viewmodel.PlayMode
import com.example.viewmodel.PuzzleSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class GamebitUpgradeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `reviewGame assigns BOOK on early theory moves and null for unplayed endgame phase`() = runBlocking {
        val engine = ChessEngine(lowMemoryMode = true)
        var state = ChessBoardState.standard()
        val ucis = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6")
        val moves = ucis.map { uci ->
            val mv = MoveGenerator.legalMoves(state).first { it.uci == uci }
            state = MoveGenerator.applyMoveUnchecked(state, mv)
            mv
        }

        val report = engine.reviewGame(
            initialState = ChessBoardState.standard(),
            moves = moves,
            useOpeningBook = true,
        )

        assertEquals(6, report.moves.size)
        assertTrue(report.moves.take(2).all { it.quality == MoveQuality.BOOK })
        assertNotNull(report.whiteOpeningAccuracy)
        assertNotNull(report.blackOpeningAccuracy)
        assertNull(report.whiteEndgameAccuracy)
        assertNull(report.blackEndgameAccuracy)
    }

    @Test
    fun `reviewGame detects forced reply when only one legal move exists`() = runBlocking {
        // After Ra8+, Kh8 has no king/pawn moves and Rc3 must block via Rc8 (exact single legal reply)
        val fen = "7k/5Kp1/6P1/8/8/2r5/8/R7 w - - 0 1"
        val start = ChessBoardState.fromFen(fen).getOrThrow()
        val ra8 = MoveGenerator.legalMoves(start).first { it.uci == "a1a8" }
        val afterCheck = MoveGenerator.applyMoveUnchecked(start, ra8)
        val replies = MoveGenerator.legalMoves(afterCheck)
        assertEquals("Expected single forced reply, got $replies", 1, replies.size)

        val engine = ChessEngine(lowMemoryMode = true)
        val report = engine.reviewGame(
            initialState = start,
            moves = listOf(ra8, replies.first()),
            useOpeningBook = false,
        )
        assertEquals(MoveQuality.FORCED, report.moves[1].quality)
    }

    @Test
    fun `EndgameClassifier classifies all 10 categories and returns null for full board`() {
        assertNull(EndgameClassifier.classify(ChessBoardState.standard()))

        val cases = listOf(
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1" to "Theoretical Draw",
            "4k3/8/8/8/8/8/8/3NK3 w - - 0 1" to "Theoretical Draw",
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1" to "Pawn Endgame",
            "4k3/3r4/8/8/8/8/3Q4/4K3 w - - 0 1" to "Queen vs Rook Endgame",
            "4k3/3n4/8/8/8/8/3Q4/4K3 w - - 0 1" to "Queen vs Minor Piece",
            "4k3/3q4/8/8/8/8/3Q4/4K3 w - - 0 1" to "Queen Endgame",
            "4k3/3r4/8/8/8/8/3R4/4K3 w - - 0 1" to "Rook Endgame",
            "4k3/3n4/8/8/8/8/3R4/4K3 w - - 0 1" to "Rook vs Minor Piece",
            "4k3/3n4/8/8/8/8/3B4/4K3 w - - 0 1" to "Bishop vs Knight",
            "4k3/3b4/8/8/8/8/3B4/4K3 w - - 0 1" to "Bishop Endgame",
            "4k3/3n4/8/8/8/8/3N4/4K3 w - - 0 1" to "Knight Endgame",
        )

        cases.forEach { (fen, expectedCategory) ->
            val st = ChessBoardState.fromFen(fen).getOrThrow()
            val result = EndgameClassifier.classify(st)
            assertNotNull("Expected endgame classification for $fen", result)
            assertEquals("Mismatch for $fen", expectedCategory, result!!.category)
        }
    }

    @Test
    fun `RatingCategoryStats and SettingsRepository clamp ratings and persist modes`() = runBlocking {
        val clampedHigh = RatingCategoryStats.decode("5000|250.0|4900|5000|100|5|5000")
        assertEquals(3200, clampedHigh.rating)
        assertEquals(3200, clampedHigh.peakRating)

        val clampedLow = RatingCategoryStats.decode("50|250.0|80|100|-30|2|50")
        assertEquals(200, clampedLow.rating)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)
        repo.setPerformanceMode(PerformanceMode.LOW_END_1GB)
        val afterLow = repo.settings.first()
        assertEquals(PerformanceMode.LOW_END_1GB, afterLow.performanceMode)
        assertTrue(afterLow.lowMemoryMode)
        assertEquals(AnimationMode.REDUCED, afterLow.animationMode)
        assertEquals(2, afterLow.engineDepth)
        assertEquals(1, afterLow.engineMultiPv)

        repo.setAnimationMode(AnimationMode.OFF)
        repo.setVoiceMode(VoiceMode.FULL)
        repo.setVoiceSpeed(1.25f)
        val updated = repo.settings.first()
        assertEquals(AnimationMode.OFF, updated.animationMode)
        assertEquals(false, updated.moveAnimation)
        assertEquals(VoiceMode.FULL, updated.voiceMode)
        assertEquals(1.25f, updated.voiceSpeed, 0.01f)

        val blitzAfterWin = repo.recordLocalGameRating("blitz", opponentRating = 1500, score = 1.0)
        assertTrue(blitzAfterWin.rating in 200..3200)
        assertEquals(1, blitzAfterWin.gamesPlayed)
    }

    @Test
    fun `EvaluationGraph renders zoom and timeline step controls`() {
        val sampleMoves = listOf(
            ReviewedMove(1, "e4", "e2e4", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 20, 35, "e4", "e2e4", 0, MoveQuality.BOOK, "Book", false),
            ReviewedMove(2, "e5", "e7e5", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 35, 25, "e5", "e7e5", 0, MoveQuality.BOOK, "Book", false),
            ReviewedMove(3, "Qh5", "d1h5", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 25, 190, "Nf3", "g1f3", 40, MoveQuality.GOOD, "Sharp", false),
            ReviewedMove(4, "g6??", "g7g6", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 190, 50_000, "Nc6", "b8c6", 500, MoveQuality.BLUNDER, "Blunder", true, mateAfter = 1),
        )
        var selected = 4

        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                EvaluationGraph(
                    reviewedMoves = sampleMoves,
                    selectedPly = selected,
                    onSelectPly = { selected = it },
                    reducedAnimations = true,
                )
            }
        }

        composeRule.onNodeWithTag("eval_graph").assertIsDisplayed()
        composeRule.onNodeWithTag("eval_graph_zoom").assertIsDisplayed().assertTextContains("1x")
        composeRule.onNodeWithTag("eval_graph_zoom").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("eval_graph_zoom").assertTextContains("2x")

        composeRule.onNodeWithTag("eval_graph_prev").performClick()
        composeRule.waitForIdle()
        assertEquals(3, selected)
    }

    @Test
    fun `PostGameComparisonDialog displays comparison table and triggers callbacks`() {
        var rematchClicked = false
        val report = GameReviewReport(
            moves = listOf(
                ReviewedMove(1, "e4", "e2e4", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 15, 25, "e4", "e2e4", 0, MoveQuality.BEST, "Best", false),
                ReviewedMove(2, "e5", "e7e5", ChessBoardState.STANDARD_FEN, ChessBoardState.STANDARD_FEN, 25, 20, "e5", "e7e5", 0, MoveQuality.BEST, "Best", false),
            ),
            whiteAccuracy = 94,
            blackAccuracy = 88,
            openingAccuracy = 91,
            middlegameAccuracy = 85,
            endgameAccuracy = 80,
            whiteInaccuracies = 0,
            whiteMistakes = 0,
            whiteBlunders = 0,
            blackInaccuracies = 1,
            blackMistakes = 0,
            blackBlunders = 0,
            whiteOpeningAccuracy = 96,
            whiteMiddlegameAccuracy = null,
            whiteEndgameAccuracy = null,
            blackOpeningAccuracy = 85,
            blackMiddlegameAccuracy = null,
            blackEndgameAccuracy = null,
            whiteAcpl = 12,
            blackAcpl = 28,
            advantageSummary = "White maintained a slight edge.",
        )

        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                PostGameComparisonDialog(
                    gameStatus = GameStatus.CHECKMATE,
                    winner = PieceColor.WHITE,
                    whiteName = "You",
                    blackName = "Tariq (650)",
                    eloDelta = null,
                    report = report,
                    reviewedMoves = report.moves,
                    isGeneratingSummary = false,
                    reducedAnimations = true,
                    savedToArchive = true,
                    onReviewGame = {},
                    onAnalyze = {},
                    onRematch = { rematchClicked = true },
                    onNewGame = {},
                    onShareGame = {},
                    onSaveGame = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("post_game_summary_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("94%").assertIsDisplayed()
        composeRule.onNodeWithText("88%").assertIsDisplayed()
        composeRule.onNodeWithTag("rematch_btn").performScrollTo().performClick()
        assertTrue(rematchClicked)
    }

    @Test
    fun `LearnScreen shows puzzle Solution button and triggers reveal callback`() {
        var solutionRevealed = false
        val puzzle = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.first()
        val puzzleBoard = ChessBoardState.fromFen(puzzle.fen).getOrThrow()
        val puzzleState = PuzzleSessionState(
            currentPuzzle = puzzle,
            boardState = puzzleBoard,
            legalMoves = MoveGenerator.legalMoves(puzzleBoard),
            feedbackText = "Find the best tactical move.",
        )

        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                LearnScreen(
                    settings = AppSettings(reducedAnimations = true, animationMode = AnimationMode.REDUCED),
                    puzzleState = puzzleState,
                    puzzleCollection = emptyList(),
                    puzzleAttempts = emptyList(),
                    lessonProgress = emptyList(),
                    drillState = OpeningDrillSessionState(),
                    coordState = CoordinateTrainerState(),
                    endgameState = EndgameTrainerState(),
                    onSelectPuzzleMode = { _, _ -> },
                    onSelectTheme = {},
                    onSelectDifficultyBand = {},
                    onSubmitPuzzleMove = {},
                    onRetryPuzzle = {},
                    onNextPuzzle = {},
                    onRevealHint = {},
                    onRevealSolution = { solutionRevealed = true },
                    onToggleBookmarkPuzzle = {},
                    onCreateCustomPuzzle = { _, _, _, _, _, _, _ -> true },
                    onDeleteCustomPuzzle = {},
                    onLoadSpecificPuzzle = {},
                    onCompleteLessonStep = { _, _ -> },
                    onStartDrill = {},
                    onSubmitDrillMove = {},
                    onStartCoordTrainer = {},
                    onTapCoordSquare = {},
                    onSelectEndgameExercise = {},
                    onSubmitEndgameMove = {},
                    onUndoEndgameMove = {},
                    onResetEndgameExercise = {},
                    onToggleEndgameCoach = {},
                    onToggleEndgameBestArrow = {},
                    onAnalyzeEndgameNow = {},
                    onPracticeEndgameInPlay = {},
                )
            }
        }

        composeRule.onNodeWithTag("puzzle_solution_btn").assertIsDisplayed().performClick()
        assertTrue(solutionRevealed)
    }

    @Test
    fun `ChessBoardView cancels off-board drag release and executes valid in-board drag`() {
        val boardState = ChessBoardState.standard()
        val legalMoves = MoveGenerator.legalMoves(boardState)
        val recordedMoves = mutableListOf<String>()

        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                ChessBoardView(
                    state = boardState,
                    legalMoves = legalMoves,
                    theme = ChessBoardThemes.first(),
                    modifier = Modifier.size(320.dp),
                    onMoveSelected = { recordedMoves.add(it.uci) },
                )
            }
        }

        val boardNode = composeRule.onNodeWithTag("chess_board")
        boardNode.assertIsDisplayed()

        // 1. Drag e2 pawn outside the bottom edge of the board and release -> must cancel without moving
        boardNode.performTouchInput {
            val cellW = width / 8f
            val cellH = height / 8f
            val e2Center = Offset(cellW * 4.5f, cellH * 6.5f)
            val offBoardBottom = Offset(cellW * 4.5f, height + 120f)
            down(e2Center)
            moveTo(offBoardBottom)
            up()
        }
        composeRule.waitForIdle()
        assertTrue("Expected off-board drag drop to cancel, but got: $recordedMoves", recordedMoves.isEmpty())

        // 2. Drag e2 pawn outside the left edge of the board and release -> must cancel without moving
        boardNode.performTouchInput {
            val cellW = width / 8f
            val cellH = height / 8f
            val e2Center = Offset(cellW * 4.5f, cellH * 6.5f)
            val offBoardLeft = Offset(-90f, cellH * 4.5f)
            down(e2Center)
            moveTo(offBoardLeft)
            up()
        }
        composeRule.waitForIdle()
        assertTrue("Expected off-left drag drop to cancel, but got: $recordedMoves", recordedMoves.isEmpty())

        // 3. Drag e2 pawn cleanly to e4 inside the board -> must execute e2e4
        boardNode.performTouchInput {
            val cellW = width / 8f
            val cellH = height / 8f
            val e2Center = Offset(cellW * 4.5f, cellH * 6.5f)
            val e4Center = Offset(cellW * 4.5f, cellH * 4.5f)
            down(e2Center)
            moveTo(e4Center)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(listOf("e2e4"), recordedMoves)
    }

    @Test
    fun `FIDE rules accurately detect same-color-bishop insufficient material and pinned en-passant repetition keys`() {
        // 1. K+B+B (same light-square color: c1 and e3) vs K is a FIDE dead draw
        val sameColorBishops = ChessBoardState.fromFen("4k3/8/8/8/8/4B3/8/2B1K3 w - - 0 1").getOrThrow()
        assertTrue(MoveGenerator.isInsufficientMaterial(sameColorBishops))

        // 2. K+B (c1 dark square) vs K+B (c8 light square) is NOT a dead draw (mate possible in corner)
        val oppositeColorBishops = ChessBoardState.fromFen("2b1k3/8/8/8/8/8/8/2B1K3 w - - 0 1").getOrThrow()
        assertFalse(MoveGenerator.isInsufficientMaterial(oppositeColorBishops))

        // 3. K+B (c1 dark square) vs K+B (e7 dark square) IS a FIDE dead draw
        val bothSameColor = ChessBoardState.fromFen("4k3/4b3/8/8/8/8/8/2B1K3 w - - 0 1").getOrThrow()
        assertTrue(MoveGenerator.isInsufficientMaterial(bothSameColor))

        // 4. Three-Check: K+N vs K can still deliver 3 checks -> ongoing; K vs K cannot check -> insufficient
        val threeCheckKnight = ChessBoardState.fromFen("4k3/8/8/8/8/8/8/4KN2 w - - 0 1", ChessVariant.THREE_CHECK).getOrThrow()
        assertFalse(MoveGenerator.isInsufficientMaterial(threeCheckKnight))
        val threeCheckBareKings = ChessBoardState.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1", ChessVariant.THREE_CHECK).getOrThrow()
        assertTrue(MoveGenerator.isInsufficientMaterial(threeCheckBareKings))

        // 5. FIDE Repetition Key: 1. e4 creates pseudo-EP e3 without any black capturer -> key matches '-' EP
        val afterE4 = MoveGenerator.applyMoveUnchecked(
            ChessBoardState.standard(),
            MoveGenerator.legalMoves(ChessBoardState.standard()).first { it.uci == "e2e4" },
        )
        val afterE4NoEp = afterE4.copy(enPassantSquare = null)
        assertEquals(afterE4NoEp.positionKey(), afterE4.positionKey())

        // 6. Horizontally pinned EP: Ka5, pawn e5, Black d5 + Rh5 -> exd6 exposes king so EP is illegal -> key ignores d6
        val pinnedEp = ChessBoardState.fromFen("4k3/8/8/K2pP2r/8/8/8/8 w - d6 0 1").getOrThrow()
        assertFalse(MoveGenerator.hasLegalEnPassantCapture(pinnedEp))
        assertEquals(pinnedEp.copy(enPassantSquare = null).positionKey(), pinnedEp.positionKey())

        // 7. Legal EP: Black pawn on d4 CAN legally capture e3 -> positionKey preserves e3
        val legalEp = ChessBoardState.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1").getOrThrow()
        assertTrue(MoveGenerator.hasLegalEnPassantCapture(legalEp))
        assertNotEquals(legalEp.copy(enPassantSquare = null).positionKey(), legalEp.positionKey())

        // 8. Atomic adjacent kings: neither king is considered in check
        val adjacentAtomic = ChessBoardState.fromFen("8/8/8/3kK3/8/8/8/8 w - - 0 1", ChessVariant.ATOMIC).getOrThrow()
        assertFalse(MoveGenerator.isKingInCheck(adjacentAtomic, PieceColor.WHITE))
        assertFalse(MoveGenerator.isKingInCheck(adjacentAtomic, PieceColor.BLACK))

        // 9. Racing Kings: White on rank 7 (b8) + Black on rank 6 (h7) has 0 legal moves -> VARIANT_WIN for White
        val rkBlocked = ChessBoardState.fromFen("1KR5/7k/2R5/4B3/8/8/8/8 b - - 0 1", ChessVariant.RACING_KINGS).getOrThrow()
        val (rkStatus, rkWinner) = MoveGenerator.evaluateGameResult(rkBlocked)
        assertEquals(GameStatus.VARIANT_WIN, rkStatus)
        assertEquals(PieceColor.WHITE, rkWinner)
    }

    @Test
    fun `all bots have distinct voice profiles, avatars, and contextual dialogue`() {
        val bots = ChessHubBots.ALL
        assertEquals(18, bots.size)
        assertEquals(18, bots.map { it.id }.toSet().size)

        // Ensure every bot has a unique (pitch, rate, timbreSlot, voiceStyle) voice signature
        val voiceSignatures = bots.map {
            "${it.voiceProfile.pitch}|${it.voiceProfile.rate}|${it.voiceProfile.timbreSlot}|${it.voiceProfile.voiceStyle}"
        }.toSet()
        assertEquals(bots.size, voiceSignatures.size)

        // Ensure every bot has an illustrated avatar spec with distinct primary/accent palette
        val avatarPalettes = bots.map { "${it.avatarSpec.primaryHex}|${it.avatarSpec.accentHex}|${it.avatarSpec.emblemStyle}" }.toSet()
        assertEquals(bots.size, avatarPalettes.size)

        // Verify contextual dialogues adapt to personality and chess events
        val sharpBot = bots.first { it.id == "bot_club" }
        val calmBot = bots.first { it.id == "bot_positional" }
        val startSharp = sharpBot.dialogueFor("start", 0)
        val startCalm = calmBot.dialogueFor("start", 0)
        assertNotEquals(startSharp, startCalm)

        val openingLine = calmBot.dialogueFor("opening", 6, openingName = "Ruy Lopez")
        assertTrue(openingLine.contains("Ruy Lopez"))

        val blunderLine = sharpBot.dialogueFor("blunder_opp", 12, san = "Nxf7")
        assertTrue(blunderLine.isNotBlank())

        val promoLine = sharpBot.dialogueFor("promotion", 34, san = "e8=Q+")
        assertTrue(promoLine.isNotBlank())

        assertEquals(12, PlayerAvatarCatalog.ALL.size)
        assertEquals(12, PlayerAvatarCatalog.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun `PlayScreen displays bot avatar and dialogue voice banner`() {
        var spoken = false
        val selectedBot = ChessHubBots.ALL.first { it.id == "bot_tactical" }
        val localState = LocalGameUiState(
            bot = selectedBot,
            botDialogueText = selectedBot.dialogueFor("start", 0),
            timeControl = TimeControl.PRESETS[3],
        )

        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                PlayScreen(
                    localState = localState,
                    settings = AppSettings(localAvatarId = "cyber", animationMode = AnimationMode.FULL),
                    isOnline = false,
                    currentUserId = null,
                    myProfile = null,
                    searchingTicketId = null,
                    activeOnlineGame = null,
                    onlineChatMessages = emptyList(),
                    onlineRatingDelta = null,
                    onLocalMove = {},
                    onQueuePremove = {},
                    onClearPremoves = {},
                    onConfirmMove = {},
                    onCancelConfirmMove = {},
                    onSelectDropPiece = {},
                    onStartNewLocalGame = { _, _, _, _, _, _ -> },
                    onToggleBotVsBotPause = {},
                    onStepBotVsBotNext = {},
                    onToggleFlip = {},
                    onToggleClockPause = {},
                    onTakeback = {},
                    onRequestHint = {},
                    onResignLocal = {},
                    onAgreeDrawLocal = {},
                    onAnalyzePgn = {},
                    onShareText = { _, _ -> },
                    onStartMatchmaking = { _, _, _ -> },
                    onCancelMatchmaking = {},
                    onOnlineMove = {},
                    onResignOnline = {},
                    onOfferOnlineDraw = {},
                    onSendOnlineChat = {},
                    onCloseOnlineGame = {},
                    onRequestOnlineSignIn = {},
                    onSpeakBotDialogue = { spoken = true },
                )
            }
        }

        composeRule.onNodeWithTag("bot_dialogue_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bot_speak_btn").performClick()
        assertTrue(spoken)
    }

    @Test
    fun `ProfileAndLookupTab displays all 12 player crests and selects cyber avatar`() {
        var chosenAvatar = "king"
        composeRule.setContent {
            ChessHubTheme(darkTheme = true) {
                ProfileAndLookupTab(
                    isOnline = false,
                    settings = AppSettings(localAvatarId = chosenAvatar),
                    myProfile = null,
                    externalSummary = null,
                    onRequestSignIn = {},
                    onUpdateProfile = { _, _, _, _, _ -> },
                    onUpdateLocalIdentity = { _, _ -> },
                    onAvatarSelected = { chosenAvatar = it },
                    onLookupExternalPlayer = { _, _ -> },
                    onImportExternalPgn = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithTag("avatar_option_cyber").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("cyber", chosenAvatar)
    }
}
