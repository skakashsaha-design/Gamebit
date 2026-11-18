package com.example

import com.example.auth.AuthRepository
import com.example.auth.HttpAuthRepository
import com.example.chess.ChessBoardState
import com.example.chess.ChessVariant
import com.example.chess.Move
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.chess.TimeControl
import com.example.data.local.AppSettings
import com.example.data.local.LocalGameEntity
import com.example.data.remote.AbuseReportDoc
import com.example.data.remote.ChessHubWebSocketClient
import com.example.data.remote.ChessHubWsEvent
import com.example.data.remote.CloudStudyDoc
import com.example.data.remote.ClubDoc
import com.example.data.remote.DirectMessageDoc
import com.example.data.remote.ExternalChessApis
import com.example.data.remote.FriendshipDoc
import com.example.data.remote.GameActionResponse
import com.example.data.remote.GameChatMessageDoc
import com.example.data.remote.MatchTicketDoc
import com.example.data.remote.MatchmakingResponse
import com.example.data.remote.MessageThreadDoc
import com.example.data.remote.OnlineChessRepository
import com.example.data.remote.OnlineGameDoc
import com.example.data.remote.PublicProfileDoc
import com.example.data.remote.RatingDeltaResult
import com.example.data.remote.RatingHistoryPoint
import com.example.data.remote.SubmitMoveResponse
import com.example.data.remote.TeamDoc
import com.example.data.remote.TournamentDoc
import com.example.data.remote.TournamentEntryDoc
import com.example.data.remote.WebSocketConnectionState
import com.example.ui.screens.computeChessHubAchievements
import com.example.viewmodel.OnlineViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineBackendAndAchievementsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun webSocketUrlAndEventParsingWorkWithoutFirebase() {
        assertEquals("wss://api.chesshub.org/v1/ws", ChessHubWebSocketClient.toWebSocketUrl("https://api.chesshub.org/"))
        assertEquals("ws://10.0.2.2:8080/v1/ws", ChessHubWebSocketClient.toWebSocketUrl("http://10.0.2.2:8080"))

        val clockEvent = ChessHubWebSocketClient.parseServerEvent(
            """{"type":"clock_sync","gameId":"g1","whiteTimeMs":295000,"blackTimeMs":300000,"turn":"b","serverTimestampMs":1700000000000}""",
        ) as? ChessHubWsEvent.ClockSync
        assertNotNull(clockEvent)
        assertEquals("g1", clockEvent?.gameId)
        assertEquals(295000L, clockEvent?.whiteTimeMs)
        assertEquals("b", clockEvent?.turn)

        val moveEvent = ChessHubWebSocketClient.parseServerEvent(
            """{"type":"move_broadcast","game":{"id":"g1","whiteUid":"u1","blackUid":"u2","status":"active","fen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1","moveCount":1,"turn":"b","whiteTimeMs":298000,"blackTimeMs":300000},"ratingDelta":null}""",
        ) as? ChessHubWsEvent.MoveBroadcast
        assertNotNull(moveEvent)
        assertEquals("g1", moveEvent?.game?.id)
        assertEquals(1L, moveEvent?.game?.moveCount)
    }

    @Test
    fun authSessionParsingAndUrlNormalizationWork() {
        assertEquals("https://play.chesshub.org", AuthRepository.normalizeServerUrl("play.chesshub.org/"))
        assertEquals("http://localhost:8080", AuthRepository.normalizeServerUrl("localhost:8080"))

        val session = HttpAuthRepository.parseSessionResponse(
            """{"accessToken":"jwt-abc","user":{"userId":"u42","username":"GrandMaster42","countryIso":"BD","fideFederation":"BAN"}}""",
            "https://api.chesshub.org",
        )
        assertEquals("jwt-abc", session.accessToken)
        assertEquals("u42", session.user.uid)
        assertEquals("GrandMaster42", session.user.username)
        assertEquals("ChessHub error", HttpAuthRepository.parseServerError("""{"message":"ChessHub error"}""", 401, "https://api.chesshub.org"))
    }

    @Test
    fun onlineViewModelSubmitsServerAuthoritativeMoveAndReceivesDelta() = runTest {
        val fakeRepo = FakeOnlineChessRepository()
        val vm = OnlineViewModel(
            repo = fakeRepo,
            externalApis = ExternalChessApis(),
            currentUserId = "u1",
            initialDisplayName = "Player1",
            countryIso = "BD",
            fideFederation = "BAN",
        )
        advanceUntilIdle()

        vm.startQuickMatchmaking(ChessVariant.STANDARD, TimeControl.PRESETS[3], true)
        advanceUntilIdle()

        assertEquals("g_online_1", vm.activeGameId.value)
        assertNotNull(vm.activeGameDoc.value)

        vm.playOnlineMove(Move(Square.fromAlgebraic("e2")!!, Square.fromAlgebraic("e4")!!))
        advanceUntilIdle()

        assertEquals("e2e4", fakeRepo.lastSubmittedUci)
        assertEquals(1L, vm.activeGameDoc.value?.moveCount)
        assertEquals(16, vm.lastRatingDelta.value?.delta)
    }

    @Test
    fun achievementsReflectRealLocalAndOnlineActivity() {
        val games = listOf(
            sampleGame("1", "You", "Bot A", "1-0", "Checkmate"),
            sampleGame("2", "You", "Bot B", "1-0", "Resignation"),
            sampleGame("3", "Bot C", "You", "0-1", "Checkmate"),
        )
        val achievements = computeChessHubAchievements(games, null, AppSettings(localPuzzleRating = 1500, puzzleBestStorm = 12))
        val byId = achievements.associateBy { it.id }

        assertTrue(byId.getValue("first_win").unlocked)
        assertTrue(byId.getValue("first_checkmate").unlocked)
        assertTrue(byId.getValue("win_streak").unlocked)
        assertTrue(byId.getValue("rating_milestone").unlocked)
        assertTrue(byId.getValue("speed_solver").unlocked)
        assertFalse(byId.getValue("ten_wins").unlocked)
        assertFalse(byId.getValue("fifty_games").unlocked)
        assertFalse(byId.getValue("hundred_games").unlocked)
    }

    private fun sampleGame(id: String, white: String, black: String, result: String, reason: String) = LocalGameEntity(
        id = id,
        whiteName = white,
        blackName = black,
        whiteRating = 1500,
        blackRating = 1500,
        result = result,
        resultReason = reason,
        variant = "Standard",
        timeControl = "5+0",
        mode = "vs_bot",
        openingEco = "C20",
        openingName = "King's Pawn",
        initialFen = ChessBoardState.STANDARD_FEN,
        finalFen = ChessBoardState.STANDARD_FEN,
        pgn = "1. e4 e5",
        createdAt = id.toLong(),
    )

    private class FakeOnlineChessRepository : OnlineChessRepository {
        override val connectionState: StateFlow<WebSocketConnectionState> = MutableStateFlow(WebSocketConnectionState.CONNECTED)
        var lastSubmittedUci: String? = null
        private val gameFlow = MutableStateFlow<OnlineGameDoc?>(
            OnlineGameDoc(
                id = "g_online_1",
                whiteUid = "u1",
                blackUid = "u2",
                whiteName = "Player1",
                blackName = "Opponent",
                participantUids = listOf("u1", "u2"),
                fen = ChessBoardState.STANDARD_FEN,
                status = "active",
                moveCount = 0L,
                turn = "w",
            ),
        )

        override fun connectRealtime() {}
        override fun disconnectRealtime() {}
        override suspend fun reconnectActiveGame(gameId: String): Result<OnlineGameDoc> = Result.success(gameFlow.value!!)
        override suspend fun ensureUserInitialized(username: String, countryIso: String, fideFederation: String): Result<PublicProfileDoc> =
            Result.success(PublicProfileDoc(userId = "u1", username = username))
        override fun observeMyProfile(uid: String): Flow<PublicProfileDoc?> = flowOf(PublicProfileDoc(userId = uid, username = "Player1"))
        override fun observeLeaderboard(): Flow<List<PublicProfileDoc>> = flowOf(emptyList())
        override suspend fun fetchRatingHistory(uid: String): Result<List<RatingHistoryPoint>> = Result.success(emptyList())
        override suspend fun searchPublicPlayers(queryText: String): Result<List<PublicProfileDoc>> = Result.success(emptyList())
        override suspend fun updateProfileIdentity(username: String, bio: String, countryIso: String, fideFederation: String, publicVisible: Boolean): Result<PublicProfileDoc> =
            Result.success(PublicProfileDoc(userId = "u1", username = username))
        override fun observeMyGames(uid: String): Flow<List<OnlineGameDoc>> = flowOf(emptyList())
        override fun observeLiveWatchGames(): Flow<List<OnlineGameDoc>> = flowOf(emptyList())
        override fun observeGame(gameId: String): Flow<OnlineGameDoc?> = gameFlow
        override suspend fun findOrCreateMatch(variant: ChessVariant, timeControl: String, initialTimeMs: Long, incrementMs: Long, delayMs: Long, ratedGame: Boolean): Result<MatchmakingResponse> =
            Result.success(MatchmakingResponse(status = "matched", game = gameFlow.value))
        override fun observeMatchTicket(ticketId: String): Flow<MatchTicketDoc?> = flowOf(null)
        override suspend fun cancelMatchTicket(ticketId: String): Result<Unit> = Result.success(Unit)
        override suspend fun createFriendChallengeGame(friendUid: String, variant: ChessVariant, timeControl: String, initialTimeMs: Long, incrementMs: Long, ratedGame: Boolean): Result<OnlineGameDoc> =
            Result.success(gameFlow.value!!)
        override suspend fun submitOnlineMove(gameId: String, moveUci: String, expectedMoveCount: Long): Result<SubmitMoveResponse> {
            lastSubmittedUci = moveUci
            val updated = gameFlow.value!!.copy(moveCount = expectedMoveCount + 1, turn = "b", lastMoveUci = moveUci)
            gameFlow.value = updated
            return Result.success(SubmitMoveResponse(game = updated, ratingDelta = RatingDeltaResult(true, "blitz", 1500, 1516)))
        }
        override suspend fun resignOnlineGame(gameId: String): Result<GameActionResponse> =
            Result.success(GameActionResponse(gameFlow.value!!.copy(status = "finished", result = "0-1")))
        override suspend fun offerOrAcceptDraw(gameId: String): Result<GameActionResponse> =
            Result.success(GameActionResponse(gameFlow.value!!.copy(drawOfferBy = "u1")))
        override fun observeGameChat(gameId: String): Flow<List<GameChatMessageDoc>> = flowOf(emptyList())
        override suspend fun sendGameChat(gameId: String, text: String): Result<GameChatMessageDoc> =
            Result.success(GameChatMessageDoc(id = "c1", senderUid = "u1", senderName = "Player1", text = text))
        override fun observeFriendships(uid: String): Flow<List<FriendshipDoc>> = flowOf(emptyList())
        override suspend fun sendFriendRequest(recipientUid: String): Result<FriendshipDoc> = Result.success(FriendshipDoc())
        override suspend fun updateFriendshipStatus(friendshipId: String, status: String): Result<FriendshipDoc> = Result.success(FriendshipDoc())
        override fun observeMessageThreads(uid: String): Flow<List<MessageThreadDoc>> = flowOf(emptyList())
        override fun observeDirectMessages(threadId: String): Flow<List<DirectMessageDoc>> = flowOf(emptyList())
        override suspend fun sendDirectMessage(recipientUid: String, text: String): Result<DirectMessageDoc> = Result.success(DirectMessageDoc())
        override fun observePublicStudies(): Flow<List<CloudStudyDoc>> = flowOf(emptyList())
        override suspend fun publishCloudStudy(title: String, description: String, pgn: String, tags: List<String>, publicVisible: Boolean, chapterCount: Int): Result<String> = Result.success("s1")
        override fun observeClubs(): Flow<List<ClubDoc>> = flowOf(emptyList())
        override suspend fun createClub(name: String, description: String, countryIso: String): Result<ClubDoc> = Result.success(ClubDoc())
        override suspend fun joinClub(club: ClubDoc): Result<ClubDoc> = Result.success(club)
        override fun observeTeams(): Flow<List<TeamDoc>> = flowOf(emptyList())
        override suspend fun createTeam(name: String, description: String): Result<TeamDoc> = Result.success(TeamDoc())
        override fun observeTournaments(): Flow<List<TournamentDoc>> = flowOf(emptyList())
        override fun observeTournamentEntries(tournamentId: String): Flow<List<TournamentEntryDoc>> = flowOf(emptyList())
        override suspend fun createTournament(name: String, format: String, variant: String, timeControl: String, ratedGame: Boolean, maxRounds: Int): Result<TournamentDoc> =
            Result.success(TournamentDoc())
        override suspend fun joinTournament(tournament: TournamentDoc): Result<TournamentDoc> = Result.success(tournament)
        override suspend fun updatePresence(onlineStatus: String, activeGameId: String?) {}
        override suspend fun submitAbuseReport(targetUid: String, category: String, details: String): Result<Unit> = Result.success(Unit)
        override suspend fun checkIsAdmin(): Boolean = false
        override fun observeAdminReports(): Flow<List<AbuseReportDoc>> = flowOf(emptyList())
        override suspend fun resolveReport(reportId: String, status: String): Result<Unit> = Result.success(Unit)
    }
}
