package com.example.data.remote

import com.example.chess.ChessVariant
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Modular repository abstraction for ChessHub Online multiplayer, matchmaking,
 * server-authoritative move/clock/rating enforcement, social, and tournaments.
 */
interface OnlineChessRepository {
    val connectionState: StateFlow<WebSocketConnectionState>

    fun connectRealtime()
    fun disconnectRealtime()
    suspend fun reconnectActiveGame(gameId: String): Result<OnlineGameDoc>

    suspend fun ensureUserInitialized(
        username: String,
        countryIso: String,
        fideFederation: String,
    ): Result<PublicProfileDoc>

    fun observeMyProfile(uid: String): Flow<PublicProfileDoc?>
    fun observeLeaderboard(): Flow<List<PublicProfileDoc>>
    suspend fun fetchRatingHistory(uid: String): Result<List<RatingHistoryPoint>>
    suspend fun searchPublicPlayers(queryText: String): Result<List<PublicProfileDoc>>

    suspend fun updateProfileIdentity(
        username: String,
        bio: String,
        countryIso: String,
        fideFederation: String,
        publicVisible: Boolean,
    ): Result<PublicProfileDoc>

    fun observeMyGames(uid: String): Flow<List<OnlineGameDoc>>
    fun observeLiveWatchGames(): Flow<List<OnlineGameDoc>>
    fun observeGame(gameId: String): Flow<OnlineGameDoc?>

    suspend fun findOrCreateMatch(
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        delayMs: Long,
        ratedGame: Boolean,
    ): Result<MatchmakingResponse>

    fun observeMatchTicket(ticketId: String): Flow<MatchTicketDoc?>
    suspend fun cancelMatchTicket(ticketId: String): Result<Unit>

    suspend fun createFriendChallengeGame(
        friendUid: String,
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        ratedGame: Boolean,
    ): Result<OnlineGameDoc>

    /**
     * Sends candidate move UCI to the server. Server validates legality, updates clocks,
     * checks terminal rules, and computes ELO/Glicko-2 deltas.
     */
    suspend fun submitOnlineMove(
        gameId: String,
        moveUci: String,
        expectedMoveCount: Long,
    ): Result<SubmitMoveResponse>

    suspend fun resignOnlineGame(gameId: String): Result<GameActionResponse>
    suspend fun offerOrAcceptDraw(gameId: String): Result<GameActionResponse>

    fun observeGameChat(gameId: String): Flow<List<GameChatMessageDoc>>
    suspend fun sendGameChat(gameId: String, text: String): Result<GameChatMessageDoc>

    fun observeFriendships(uid: String): Flow<List<FriendshipDoc>>
    suspend fun sendFriendRequest(recipientUid: String): Result<FriendshipDoc>
    suspend fun updateFriendshipStatus(friendshipId: String, status: String): Result<FriendshipDoc>

    fun observeMessageThreads(uid: String): Flow<List<MessageThreadDoc>>
    fun observeDirectMessages(threadId: String): Flow<List<DirectMessageDoc>>
    suspend fun sendDirectMessage(recipientUid: String, text: String): Result<DirectMessageDoc>

    fun observePublicStudies(): Flow<List<CloudStudyDoc>>
    suspend fun publishCloudStudy(
        title: String,
        description: String,
        pgn: String,
        tags: List<String>,
        publicVisible: Boolean,
        chapterCount: Int,
    ): Result<String>

    fun observeClubs(): Flow<List<ClubDoc>>
    suspend fun createClub(name: String, description: String, countryIso: String): Result<ClubDoc>
    suspend fun joinClub(club: ClubDoc): Result<ClubDoc>

    fun observeTeams(): Flow<List<TeamDoc>>
    suspend fun createTeam(name: String, description: String): Result<TeamDoc>

    fun observeTournaments(): Flow<List<TournamentDoc>>
    fun observeTournamentEntries(tournamentId: String): Flow<List<TournamentEntryDoc>>
    suspend fun createTournament(
        name: String,
        format: String,
        variant: String,
        timeControl: String,
        ratedGame: Boolean,
        maxRounds: Int,
    ): Result<TournamentDoc>
    suspend fun joinTournament(tournament: TournamentDoc): Result<TournamentDoc>

    suspend fun updatePresence(onlineStatus: String, activeGameId: String? = null)
    suspend fun submitAbuseReport(targetUid: String, category: String, details: String): Result<Unit>
    suspend fun checkIsAdmin(): Boolean
    fun observeAdminReports(): Flow<List<AbuseReportDoc>>
    suspend fun resolveReport(reportId: String, status: String): Result<Unit>
}

class RemoteChessHubOnlineRepository(
    private val serverUrl: String,
    private val accessTokenProvider: () -> String,
    private val currentUserId: String,
    okHttpClient: OkHttpClient = ChessHubApiClientFactory.createOkHttpClient(accessTokenProvider),
    moshi: Moshi = ChessHubApiClientFactory.createMoshi(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OnlineChessRepository {

    private val api: ChessHubBackendApi = ChessHubApiClientFactory.createApi(serverUrl, okHttpClient, moshi)
    private val wsClient = ChessHubWebSocketClient(serverUrl, accessTokenProvider, okHttpClient, moshi)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val connectionState: StateFlow<WebSocketConnectionState> = wsClient.connectionState

    private val liveGamesCache = mutableMapOf<String, MutableStateFlow<OnlineGameDoc?>>()
    private val liveChatsCache = mutableMapOf<String, MutableStateFlow<List<GameChatMessageDoc>>>()
    private val liveTicketsCache = mutableMapOf<String, MutableStateFlow<MatchTicketDoc?>>()
    private val liveDmCache = mutableMapOf<String, MutableStateFlow<List<DirectMessageDoc>>>()

    init {
        scope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is ChessHubWsEvent.GameSnapshot -> {
                        gameFlowFor(event.game.id).value = event.game
                    }
                    is ChessHubWsEvent.MoveBroadcast -> {
                        gameFlowFor(event.game.id).value = event.game
                    }
                    is ChessHubWsEvent.GameEnded -> {
                        gameFlowFor(event.game.id).value = event.game
                    }
                    is ChessHubWsEvent.ClockSync -> {
                        val flow = gameFlowFor(event.gameId)
                        flow.value?.let { curr ->
                            flow.value = curr.copy(
                                whiteTimeMs = event.whiteTimeMs,
                                blackTimeMs = event.blackTimeMs,
                                turn = event.turn,
                                serverTimestampMs = event.serverTimestampMs,
                            )
                        }
                    }
                    is ChessHubWsEvent.MatchFound -> {
                        gameFlowFor(event.game.id).value = event.game
                        val tFlow = ticketFlowFor(event.ticketId)
                        val existing = tFlow.value
                        tFlow.value = (existing ?: MatchTicketDoc(id = event.ticketId, userId = currentUserId))
                            .copy(status = "matched", matchedGameId = event.game.id)
                    }
                    is ChessHubWsEvent.GameChatReceived -> {
                        val chatFlow = chatFlowFor(event.gameId)
                        if (chatFlow.value.none { it.id == event.message.id }) {
                            chatFlow.value = chatFlow.value + event.message
                        }
                    }
                    is ChessHubWsEvent.DirectMessageReceived -> {
                        val dmFlow = dmFlowFor(event.threadId)
                        if (dmFlow.value.none { it.id == event.message.id }) {
                            dmFlow.value = dmFlow.value + event.message
                        }
                    }
                    is ChessHubWsEvent.Connected,
                    is ChessHubWsEvent.PresenceChanged,
                    is ChessHubWsEvent.ServerError,
                    -> Unit
                }
            }
        }
    }

    override fun connectRealtime() {
        wsClient.connect()
    }

    override fun disconnectRealtime() {
        wsClient.disconnect()
    }

    override suspend fun reconnectActiveGame(gameId: String): Result<OnlineGameDoc> =
        runBackend("reconnect game") {
            wsClient.connect()
            wsClient.subscribeGame(gameId)
            val snapshot = api.getGame(gameId)
            gameFlowFor(gameId).value = snapshot
            snapshot
        }

    override suspend fun ensureUserInitialized(
        username: String,
        countryIso: String,
        fideFederation: String,
    ): Result<PublicProfileDoc> = runBackend("bootstrap user") {
        api.bootstrapUser(
            BootstrapUserRequest(
                username = username.trim().take(36).ifBlank { "Player_${currentUserId.take(6)}" },
                countryIso = countryIso,
                fideFederation = fideFederation,
            ),
        )
    }

    override fun observeMyProfile(uid: String): Flow<PublicProfileDoc?> =
        pollFlow("profile", intervalMs = 20_000L) { api.getMyProfile() }

    override fun observeLeaderboard(): Flow<List<PublicProfileDoc>> =
        pollFlow("leaderboard", intervalMs = 30_000L) { api.getLeaderboard(50) }

    override suspend fun fetchRatingHistory(uid: String): Result<List<RatingHistoryPoint>> =
        runBackend("rating history") { api.getRatingHistory(uid, 40) }

    override suspend fun searchPublicPlayers(queryText: String): Result<List<PublicProfileDoc>> =
        runBackend("search players") { api.searchPlayers(queryText.trim()) }

    override suspend fun updateProfileIdentity(
        username: String,
        bio: String,
        countryIso: String,
        fideFederation: String,
        publicVisible: Boolean,
    ): Result<PublicProfileDoc> = runBackend("update profile") {
        val cleanName = username.trim().take(36)
        require(cleanName.length >= 2) { "Username must be at least 2 characters." }
        api.updateMyProfile(
            UpdateProfileRequest(
                username = cleanName,
                bio = bio.trim().take(240),
                countryIso = countryIso,
                fideFederation = fideFederation,
                publicVisible = publicVisible,
            ),
        )
    }

    override fun observeMyGames(uid: String): Flow<List<OnlineGameDoc>> =
        pollFlow("my games", intervalMs = 15_000L) { api.getMyGames(30) }

    override fun observeLiveWatchGames(): Flow<List<OnlineGameDoc>> =
        pollFlow("live games", intervalMs = 12_000L) { api.getLiveWatchGames(20) }

    override fun observeGame(gameId: String): Flow<OnlineGameDoc?> = channelFlow {
        wsClient.connect()
        wsClient.subscribeGame(gameId)
        val cached = gameFlowFor(gameId)
        launch { cached.collect { send(it) } }
        launch {
            while (isActive) {
                runCatching { api.getGame(gameId) }
                    .onSuccess { cached.value = it }
                delay(if (wsClient.connectionState.value == WebSocketConnectionState.CONNECTED) 12_000L else 3_000L)
            }
        }
    }

    override suspend fun findOrCreateMatch(
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        delayMs: Long,
        ratedGame: Boolean,
    ): Result<MatchmakingResponse> = runBackend("matchmaking") {
        wsClient.connect()
        val res = api.createOrJoinMatchmaking(
            MatchmakingRequest(
                variant = variant.name,
                timeControl = timeControl,
                initialTimeMs = initialTimeMs,
                incrementMs = incrementMs,
                delayMs = delayMs,
                ratedGame = ratedGame,
            ),
        )
        res.ticket?.let {
            ticketFlowFor(it.id).value = it
            wsClient.subscribeMatchmaking(it.id)
        }
        res.game?.let { gameFlowFor(it.id).value = it }
        res
    }

    override fun observeMatchTicket(ticketId: String): Flow<MatchTicketDoc?> = channelFlow {
        wsClient.connect()
        wsClient.subscribeMatchmaking(ticketId)
        val cached = ticketFlowFor(ticketId)
        launch { cached.collect { send(it) } }
        launch {
            while (isActive) {
                runCatching { api.getMatchTicket(ticketId) }
                    .onSuccess { cached.value = it }
                delay(2_500L)
            }
        }
    }

    override suspend fun cancelMatchTicket(ticketId: String): Result<Unit> =
        runBackend("cancel matchmaking") {
            api.cancelMatchTicket(ticketId)
        }

    override suspend fun createFriendChallengeGame(
        friendUid: String,
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        ratedGame: Boolean,
    ): Result<OnlineGameDoc> = runBackend("friend challenge") {
        val game = api.createChallengeGame(
            CreateChallengeRequest(
                opponentUid = friendUid,
                variant = variant.name,
                timeControl = timeControl,
                initialTimeMs = initialTimeMs,
                incrementMs = incrementMs,
                ratedGame = ratedGame,
            ),
        )
        gameFlowFor(game.id).value = game
        wsClient.connect()
        wsClient.subscribeGame(game.id)
        game
    }

    override suspend fun submitOnlineMove(
        gameId: String,
        moveUci: String,
        expectedMoveCount: Long,
    ): Result<SubmitMoveResponse> = runBackend("submit move") {
        val response = api.submitMove(
            gameId = gameId,
            body = SubmitMoveRequest(
                moveUci = moveUci,
                expectedMoveCount = expectedMoveCount,
                clientTimestampMs = System.currentTimeMillis(),
            ),
        )
        gameFlowFor(gameId).value = response.game
        response
    }

    override suspend fun resignOnlineGame(gameId: String): Result<GameActionResponse> =
        runBackend("resign") {
            val response = api.resignGame(gameId)
            gameFlowFor(gameId).value = response.game
            response
        }

    override suspend fun offerOrAcceptDraw(gameId: String): Result<GameActionResponse> =
        runBackend("draw action") {
            val response = api.offerOrAcceptDraw(gameId)
            gameFlowFor(gameId).value = response.game
            response
        }

    override fun observeGameChat(gameId: String): Flow<List<GameChatMessageDoc>> = channelFlow {
        val cached = chatFlowFor(gameId)
        launch { cached.collect { send(it) } }
        launch {
            while (isActive) {
                runCatching { api.getGameChat(gameId, 50) }
                    .onSuccess { cached.value = it }
                delay(10_000L)
            }
        }
    }

    override suspend fun sendGameChat(gameId: String, text: String): Result<GameChatMessageDoc> =
        runBackend("game chat") {
            val clean = text.trim().take(300)
            require(clean.isNotEmpty()) { "Message cannot be empty." }
            val msg = api.postGameChat(gameId, GameChatRequest(clean))
            val flow = chatFlowFor(gameId)
            if (flow.value.none { it.id == msg.id }) flow.value = flow.value + msg
            msg
        }

    override fun observeFriendships(uid: String): Flow<List<FriendshipDoc>> =
        pollFlow("friends", intervalMs = 15_000L) { api.getFriendships() }

    override suspend fun sendFriendRequest(recipientUid: String): Result<FriendshipDoc> =
        runBackend("friend request") {
            require(recipientUid != currentUserId) { "Cannot add yourself." }
            api.sendFriendRequest(FriendRequestBody(recipientUid))
        }

    override suspend fun updateFriendshipStatus(friendshipId: String, status: String): Result<FriendshipDoc> =
        runBackend("friendship status") {
            api.updateFriendshipStatus(friendshipId, FriendshipStatusRequest(status))
        }

    override fun observeMessageThreads(uid: String): Flow<List<MessageThreadDoc>> =
        pollFlow("message threads", intervalMs = 15_000L) { api.getMessageThreads() }

    override fun observeDirectMessages(threadId: String): Flow<List<DirectMessageDoc>> = channelFlow {
        val cached = dmFlowFor(threadId)
        launch { cached.collect { send(it) } }
        launch {
            while (isActive) {
                runCatching { api.getDirectMessages(threadId, 50) }
                    .onSuccess { cached.value = it }
                delay(8_000L)
            }
        }
    }

    override suspend fun sendDirectMessage(recipientUid: String, text: String): Result<DirectMessageDoc> =
        runBackend("direct message") {
            val clean = text.trim().take(1000)
            require(clean.isNotEmpty()) { "Message cannot be empty." }
            val msg = api.sendDirectMessage(DirectMessageRequest(recipientUid, clean))
            val tid = if (currentUserId < recipientUid) "${currentUserId}_$recipientUid" else "${recipientUid}_$currentUserId"
            val flow = dmFlowFor(tid)
            if (flow.value.none { it.id == msg.id }) flow.value = flow.value + msg
            msg
        }

    override fun observePublicStudies(): Flow<List<CloudStudyDoc>> =
        pollFlow("public studies", intervalMs = 30_000L) { api.getPublicStudies(30) }

    override suspend fun publishCloudStudy(
        title: String,
        description: String,
        pgn: String,
        tags: List<String>,
        publicVisible: Boolean,
        chapterCount: Int,
    ): Result<String> = runBackend("publish study") {
        api.publishStudy(
            PublishStudyRequest(
                title = title.trim().take(120).ifBlank { "Untitled Study" },
                description = description.trim().take(1000),
                pgn = pgn.take(90_000),
                tags = tags.take(10),
                publicVisible = publicVisible,
                chapterCount = chapterCount.coerceIn(1, 64),
            ),
        ).id
    }

    override fun observeClubs(): Flow<List<ClubDoc>> =
        pollFlow("clubs", intervalMs = 30_000L) { api.getClubs(30) }

    override suspend fun createClub(name: String, description: String, countryIso: String): Result<ClubDoc> =
        runBackend("create club") {
            api.createClub(CreateClubRequest(name.trim().take(80), description.trim().take(500), countryIso))
        }

    override suspend fun joinClub(club: ClubDoc): Result<ClubDoc> =
        runBackend("join club") { api.joinClub(club.id) }

    override fun observeTeams(): Flow<List<TeamDoc>> =
        pollFlow("teams", intervalMs = 30_000L) { api.getTeams(30) }

    override suspend fun createTeam(name: String, description: String): Result<TeamDoc> =
        runBackend("create team") {
            api.createTeam(CreateTeamRequest(name.trim().take(80), description.trim().take(500)))
        }

    override fun observeTournaments(): Flow<List<TournamentDoc>> =
        pollFlow("tournaments", intervalMs = 20_000L) { api.getTournaments(30) }

    override fun observeTournamentEntries(tournamentId: String): Flow<List<TournamentEntryDoc>> =
        pollFlow("tournament standings", intervalMs = 10_000L) { api.getTournamentStandings(tournamentId, 50) }

    override suspend fun createTournament(
        name: String,
        format: String,
        variant: String,
        timeControl: String,
        ratedGame: Boolean,
        maxRounds: Int,
    ): Result<TournamentDoc> = runBackend("create tournament") {
        api.createTournament(
            CreateTournamentRequest(
                name = name.trim().take(80),
                format = format,
                variant = variant,
                timeControl = timeControl,
                ratedGame = ratedGame,
                maxRounds = maxRounds.coerceIn(1, 20),
            ),
        )
    }

    override suspend fun joinTournament(tournament: TournamentDoc): Result<TournamentDoc> =
        runBackend("join tournament") { api.joinTournament(tournament.id) }

    override suspend fun updatePresence(onlineStatus: String, activeGameId: String?) {
        runCatching { api.updatePresence(PresenceUpdateRequest(onlineStatus, activeGameId)) }
    }

    override suspend fun submitAbuseReport(targetUid: String, category: String, details: String): Result<Unit> =
        runBackend("submit report") {
            require(details.trim().length >= 4) { "Please provide at least 4 characters of details." }
            api.submitReport(
                AbuseReportRequest(
                    targetUid = targetUid.ifBlank { "general" },
                    category = category.take(50),
                    details = details.trim().take(1000),
                ),
            )
            Unit
        }

    override suspend fun checkIsAdmin(): Boolean =
        runCatching { api.getAdminStatus().isAdmin }.getOrDefault(false)

    override fun observeAdminReports(): Flow<List<AbuseReportDoc>> =
        pollFlow("admin reports", intervalMs = 20_000L) { api.getAdminReports(40) }

    override suspend fun resolveReport(reportId: String, status: String): Result<Unit> =
        runBackend("resolve report") {
            api.resolveReport(reportId, ResolveReportRequest(status))
            Unit
        }

    private fun gameFlowFor(gameId: String): MutableStateFlow<OnlineGameDoc?> =
        synchronized(liveGamesCache) { liveGamesCache.getOrPut(gameId) { MutableStateFlow(null) } }

    private fun chatFlowFor(gameId: String): MutableStateFlow<List<GameChatMessageDoc>> =
        synchronized(liveChatsCache) { liveChatsCache.getOrPut(gameId) { MutableStateFlow(emptyList()) } }

    private fun ticketFlowFor(ticketId: String): MutableStateFlow<MatchTicketDoc?> =
        synchronized(liveTicketsCache) { liveTicketsCache.getOrPut(ticketId) { MutableStateFlow(null) } }

    private fun dmFlowFor(threadId: String): MutableStateFlow<List<DirectMessageDoc>> =
        synchronized(liveDmCache) { liveDmCache.getOrPut(threadId) { MutableStateFlow(emptyList()) } }

    private fun <T> pollFlow(label: String, intervalMs: Long, fetch: suspend () -> T): Flow<T> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                emit(fetch())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw BackendErrorHandler.mapError(e, label, serverUrl)
            }
            delay(intervalMs)
        }
    }

    private suspend inline fun <T> runBackend(op: String, block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(BackendErrorHandler.mapError(e, op, serverUrl))
    }
}
