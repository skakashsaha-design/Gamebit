package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.ChessBoardState
import com.example.chess.ChessVariant
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.PieceColor
import com.example.chess.TimeControl
import com.example.data.remote.AbuseReportDoc
import com.example.data.remote.CloudStudyDoc
import com.example.data.remote.ClubDoc
import com.example.data.remote.DirectMessageDoc
import com.example.data.remote.ExternalBroadcastItem
import com.example.data.remote.ExternalChessApis
import com.example.data.remote.ExternalChessNewsItem
import com.example.data.remote.ExternalPlayerSummary
import com.example.data.remote.ExternalTvChannel
import com.example.data.remote.FriendshipDoc
import com.example.data.remote.GameChatMessageDoc
import com.example.data.remote.MessageThreadDoc
import com.example.data.remote.OnlineChessRepository
import com.example.data.remote.OnlineGameDoc
import com.example.data.remote.PublicProfileDoc
import com.example.data.remote.RatingDeltaResult
import com.example.data.remote.RatingHistoryPoint
import com.example.data.remote.TeamDoc
import com.example.data.remote.TournamentDoc
import com.example.data.remote.TournamentEntryDoc
import com.example.data.remote.WebSocketConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class OnlineViewModel(
    private val repo: OnlineChessRepository,
    private val externalApis: ExternalChessApis,
    val currentUserId: String,
    initialDisplayName: String,
    countryIso: String,
    fideFederation: String,
) : ViewModel() {

    val connectionState: StateFlow<WebSocketConnectionState> = repo.connectionState

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _searchingTicketId = MutableStateFlow<String?>(null)
    val searchingTicketId: StateFlow<String?> = _searchingTicketId.asStateFlow()

    private val _activeGameId = MutableStateFlow<String?>(null)
    val activeGameId: StateFlow<String?> = _activeGameId.asStateFlow()

    private val _activeGameDoc = MutableStateFlow<OnlineGameDoc?>(null)
    val activeGameDoc: StateFlow<OnlineGameDoc?> = _activeGameDoc.asStateFlow()

    private val _activeGameChat = MutableStateFlow<List<GameChatMessageDoc>>(emptyList())
    val activeGameChat: StateFlow<List<GameChatMessageDoc>> = _activeGameChat.asStateFlow()

    private val _selectedTournamentEntries = MutableStateFlow<List<TournamentEntryDoc>>(emptyList())
    val selectedTournamentEntries: StateFlow<List<TournamentEntryDoc>> = _selectedTournamentEntries.asStateFlow()

    private val _directMessages = MutableStateFlow<List<DirectMessageDoc>>(emptyList())
    val directMessages: StateFlow<List<DirectMessageDoc>> = _directMessages.asStateFlow()

    private val _broadcasts = MutableStateFlow<List<ExternalBroadcastItem>>(emptyList())
    val broadcasts: StateFlow<List<ExternalBroadcastItem>> = _broadcasts.asStateFlow()

    private val _tvChannels = MutableStateFlow<List<ExternalTvChannel>>(emptyList())
    val tvChannels: StateFlow<List<ExternalTvChannel>> = _tvChannels.asStateFlow()

    private val _externalPlayerSummary = MutableStateFlow<ExternalPlayerSummary?>(null)
    val externalPlayerSummary: StateFlow<ExternalPlayerSummary?> = _externalPlayerSummary.asStateFlow()

    private val _playerSearchResults = MutableStateFlow<List<PublicProfileDoc>>(emptyList())
    val playerSearchResults: StateFlow<List<PublicProfileDoc>> = _playerSearchResults.asStateFlow()

    private val _lastRatingDelta = MutableStateFlow<RatingDeltaResult?>(null)
    val lastRatingDelta: StateFlow<RatingDeltaResult?> = _lastRatingDelta.asStateFlow()

    private val _ratingHistory = MutableStateFlow<List<RatingHistoryPoint>>(emptyList())
    val ratingHistory: StateFlow<List<RatingHistoryPoint>> = _ratingHistory.asStateFlow()

    private val _chessNews = MutableStateFlow<List<ExternalChessNewsItem>>(emptyList())
    val chessNews: StateFlow<List<ExternalChessNewsItem>> = _chessNews.asStateFlow()

    private val _adminReports = MutableStateFlow<List<AbuseReportDoc>>(emptyList())
    val adminReports: StateFlow<List<AbuseReportDoc>> = _adminReports.asStateFlow()

    val myProfile: StateFlow<UiState<PublicProfileDoc?>> = repo.observeMyProfile(currentUserId)
        .toUiState("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val leaderboard: StateFlow<UiState<List<PublicProfileDoc>>> = repo.observeLeaderboard()
        .toUiState("leaderboard")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myGames: StateFlow<UiState<List<OnlineGameDoc>>> = repo.observeMyGames(currentUserId)
        .toUiState("games")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val liveWatchGames: StateFlow<UiState<List<OnlineGameDoc>>> = repo.observeLiveWatchGames()
        .toUiState("live games")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val friendships: StateFlow<UiState<List<FriendshipDoc>>> = repo.observeFriendships(currentUserId)
        .toUiState("friends")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messageThreads: StateFlow<UiState<List<MessageThreadDoc>>> = repo.observeMessageThreads(currentUserId)
        .toUiState("messages")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val publicStudies: StateFlow<UiState<List<CloudStudyDoc>>> = repo.observePublicStudies()
        .toUiState("studies")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val clubs: StateFlow<UiState<List<ClubDoc>>> = repo.observeClubs()
        .toUiState("clubs")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val teams: StateFlow<UiState<List<TeamDoc>>> = repo.observeTeams()
        .toUiState("teams")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val tournaments: StateFlow<UiState<List<TournamentDoc>>> = repo.observeTournaments()
        .toUiState("tournaments")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private var activeGameJob: Job? = null
    private var activeChatJob: Job? = null
    private var ticketJob: Job? = null
    private var tournamentEntriesJob: Job? = null
    private var dmJob: Job? = null

    init {
        repo.connectRealtime()
        viewModelScope.launch {
            repo.ensureUserInitialized(initialDisplayName, countryIso, fideFederation)
                .onFailure { _statusMessage.value = "Server sync: ${it.localizedMessage}" }
            repo.updatePresence("online")
            repo.fetchRatingHistory(currentUserId).onSuccess { _ratingHistory.value = it }
            _isAdmin.value = repo.checkIsAdmin()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun reconnectActiveGame() {
        val gameId = _activeGameId.value
        if (gameId != null) {
            viewModelScope.launch {
                repo.reconnectActiveGame(gameId)
                    .onSuccess {
                        _activeGameDoc.value = it
                        _statusMessage.value = "Reconnected to live game"
                    }
                    .onFailure { _statusMessage.value = "Reconnect failed: ${it.localizedMessage}" }
            }
        } else {
            repo.connectRealtime()
        }
    }

    fun startQuickMatchmaking(
        variant: ChessVariant,
        timeControl: TimeControl,
        rated: Boolean,
    ) {
        viewModelScope.launch {
            val res = repo.findOrCreateMatch(
                variant = variant,
                timeControl = timeControl.label,
                initialTimeMs = timeControl.initialSeconds * 1000L,
                incrementMs = timeControl.incrementSeconds * 1000L,
                delayMs = timeControl.delaySeconds * 1000L,
                ratedGame = rated,
            )
            res.fold(
                onSuccess = { response ->
                    val matchedGame = response.game
                    if (response.status == "matched" && matchedGame != null) {
                        _searchingTicketId.value = null
                        openOnlineGame(matchedGame.id)
                        _statusMessage.value = "Match found!"
                    } else if (response.ticket != null) {
                        val ticketId = response.ticket.id
                        _searchingTicketId.value = ticketId
                        observeTicket(ticketId)
                        _statusMessage.value = "Searching for ${timeControl.label} ${variant.displayName} opponent..."
                    }
                },
                onFailure = { err ->
                    _statusMessage.value = "Matchmaking error: ${err.localizedMessage}"
                },
            )
        }
    }

    private fun observeTicket(ticketId: String) {
        ticketJob?.cancel()
        ticketJob = viewModelScope.launch {
            repo.observeMatchTicket(ticketId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _statusMessage.value = e.localizedMessage
                }
                .collect { ticket ->
                    if (ticket != null && ticket.status == "matched" && ticket.matchedGameId.isNotBlank()) {
                        _searchingTicketId.value = null
                        openOnlineGame(ticket.matchedGameId)
                        ticketJob?.cancel()
                    }
                }
        }
    }

    fun cancelMatchmaking() {
        val tkt = _searchingTicketId.value ?: return
        ticketJob?.cancel()
        viewModelScope.launch {
            repo.cancelMatchTicket(tkt)
            _searchingTicketId.value = null
            _statusMessage.value = "Matchmaking cancelled"
        }
    }

    fun openOnlineGame(gameId: String) {
        _activeGameId.value = gameId
        activeGameJob?.cancel()
        activeChatJob?.cancel()
        activeGameJob = viewModelScope.launch {
            repo.observeGame(gameId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _statusMessage.value = "Game sync error: ${e.localizedMessage}"
                }
                .collect { doc -> _activeGameDoc.value = doc }
        }
        activeChatJob = viewModelScope.launch {
            repo.observeGameChat(gameId)
                .catch { e -> if (e is CancellationException) throw e }
                .collect { msgs -> _activeGameChat.value = msgs }
        }
        viewModelScope.launch { repo.updatePresence("in_game", gameId) }
    }

    fun closeActiveOnlineGame() {
        activeGameJob?.cancel()
        activeChatJob?.cancel()
        _activeGameId.value = null
        _activeGameDoc.value = null
        _activeGameChat.value = emptyList()
        viewModelScope.launch { repo.updatePresence("online", null) }
    }

    fun playOnlineMove(move: Move) {
        val doc = _activeGameDoc.value ?: return
        if (doc.status != "active") return
        val variant = ChessVariant.entries.firstOrNull { it.name == doc.variant } ?: ChessVariant.STANDARD
        val state = ChessBoardState.fromFen(doc.fen, variant).getOrNull() ?: return
        val myColor = if (doc.whiteUid == currentUserId) {
            PieceColor.WHITE
        } else if (doc.blackUid == currentUserId) {
            PieceColor.BLACK
        } else {
            return
        }
        if (state.sideToMove != myColor) return

        val legal = MoveGenerator.legalMoves(state)
        val valid = legal.firstOrNull {
            it.from == move.from && it.to == move.to && it.promotion == move.promotion && it.dropPieceType == move.dropPieceType
        } ?: return

        viewModelScope.launch {
            repo.submitOnlineMove(
                gameId = doc.id,
                moveUci = valid.uci,
                expectedMoveCount = doc.moveCount,
            ).onSuccess { response ->
                _activeGameDoc.value = response.game
                if (response.ratingDelta != null) {
                    _lastRatingDelta.value = response.ratingDelta
                    repo.fetchRatingHistory(currentUserId).onSuccess { _ratingHistory.value = it }
                }
            }.onFailure {
                _statusMessage.value = "Move rejected: ${it.localizedMessage}"
            }
        }
    }

    fun resignOnlineGame() {
        val doc = _activeGameDoc.value ?: return
        if (doc.status != "active") return
        viewModelScope.launch {
            repo.resignOnlineGame(doc.id)
                .onSuccess { response ->
                    _activeGameDoc.value = response.game
                    _lastRatingDelta.value = response.ratingDelta
                    repo.fetchRatingHistory(currentUserId).onSuccess { _ratingHistory.value = it }
                }
                .onFailure { _statusMessage.value = "Resign error: ${it.localizedMessage}" }
        }
    }

    fun offerOnlineDraw() {
        val doc = _activeGameDoc.value ?: return
        viewModelScope.launch {
            repo.offerOrAcceptDraw(doc.id)
                .onSuccess { response ->
                    _activeGameDoc.value = response.game
                    if (response.ratingDelta != null) {
                        _lastRatingDelta.value = response.ratingDelta
                        repo.fetchRatingHistory(currentUserId).onSuccess { _ratingHistory.value = it }
                    } else if (response.game.drawOfferBy == currentUserId) {
                        _statusMessage.value = "Draw offer sent"
                    }
                }
                .onFailure { _statusMessage.value = "Draw request failed: ${it.localizedMessage}" }
        }
    }

    fun clearLastRatingDelta() {
        _lastRatingDelta.value = null
    }

    fun searchPlayers(query: String) {
        viewModelScope.launch {
            repo.searchPublicPlayers(query)
                .onSuccess { _playerSearchResults.value = it }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun sendGameChat(text: String) {
        val doc = _activeGameDoc.value ?: return
        viewModelScope.launch {
            repo.sendGameChat(doc.id, text)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun updateProfile(username: String, bio: String, countryIso: String, fideFed: String, publicVisible: Boolean) {
        viewModelScope.launch {
            repo.updateProfileIdentity(username, bio, countryIso, fideFed, publicVisible)
                .onSuccess { _statusMessage.value = "Profile synced with server" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun challengeFriend(friendUid: String, friendName: String, timeControl: TimeControl, variant: ChessVariant) {
        viewModelScope.launch {
            repo.createFriendChallengeGame(
                friendUid = friendUid,
                variant = variant,
                timeControl = timeControl.label,
                initialTimeMs = timeControl.initialSeconds * 1000L,
                incrementMs = timeControl.incrementSeconds * 1000L,
                ratedGame = true,
            ).onSuccess { game ->
                openOnlineGame(game.id)
                _statusMessage.value = "Challenge game started vs $friendName"
            }.onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun addFriend(recipientUid: String, recipientName: String) {
        viewModelScope.launch {
            repo.sendFriendRequest(recipientUid)
                .onSuccess { _statusMessage.value = "Friend request sent to $recipientName" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun acceptFriend(friendshipId: String) {
        viewModelScope.launch {
            repo.updateFriendshipStatus(friendshipId, "accepted")
        }
    }

    fun blockFriend(friendshipId: String) {
        viewModelScope.launch {
            repo.updateFriendshipStatus(friendshipId, "blocked")
        }
    }

    fun openMessageThread(threadId: String) {
        dmJob?.cancel()
        dmJob = viewModelScope.launch {
            repo.observeDirectMessages(threadId)
                .catch { e -> if (e is CancellationException) throw e }
                .collect { _directMessages.value = it }
        }
    }

    fun sendDirectMessage(recipientUid: String, text: String) {
        viewModelScope.launch {
            repo.sendDirectMessage(recipientUid, text)
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun publishStudy(title: String, description: String, pgn: String) {
        viewModelScope.launch {
            repo.publishCloudStudy(title, description, pgn, listOf("ChessHub"), true, 1)
                .onSuccess { _statusMessage.value = "Study published to ChessHub server" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun createClub(name: String, desc: String, countryIso: String) {
        viewModelScope.launch {
            repo.createClub(name, desc, countryIso)
                .onSuccess { _statusMessage.value = "Club created" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun joinClub(club: ClubDoc) {
        viewModelScope.launch {
            repo.joinClub(club)
                .onSuccess { _statusMessage.value = "Joined ${club.name}" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun createTeam(name: String, desc: String) {
        viewModelScope.launch {
            repo.createTeam(name, desc)
                .onSuccess { _statusMessage.value = "Team created" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun createTournament(name: String, format: String, variant: ChessVariant, timeControl: TimeControl) {
        viewModelScope.launch {
            repo.createTournament(
                name = name,
                format = format,
                variant = variant.displayName,
                timeControl = timeControl.label,
                ratedGame = true,
                maxRounds = if (format == "swiss") 5 else 10,
            ).onSuccess { _statusMessage.value = "Tournament launched" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun joinTournament(t: TournamentDoc) {
        viewModelScope.launch {
            repo.joinTournament(t)
                .onSuccess { _statusMessage.value = "Joined ${t.name}" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun observeTournamentStandings(tournamentId: String) {
        tournamentEntriesJob?.cancel()
        tournamentEntriesJob = viewModelScope.launch {
            repo.observeTournamentEntries(tournamentId)
                .catch { e -> if (e is CancellationException) throw e }
                .collect { _selectedTournamentEntries.value = it }
        }
    }

    fun loadBroadcastsAndTv() {
        viewModelScope.launch {
            externalApis.fetchLichessBroadcasts().onSuccess { _broadcasts.value = it }
            externalApis.fetchLichessTvChannels().onSuccess { _tvChannels.value = it }
            externalApis.fetchChessNews().onSuccess { _chessNews.value = it }
        }
    }

    fun lookupExternalPlayer(platform: String, username: String) {
        viewModelScope.launch {
            val res = if (platform == "Lichess") {
                externalApis.fetchLichessProfileAndGames(username)
            } else {
                externalApis.fetchChessComProfileAndGames(username)
            }
            res.onSuccess { _externalPlayerSummary.value = it }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun submitReport(targetUid: String, category: String, details: String) {
        viewModelScope.launch {
            repo.submitAbuseReport(targetUid, category, details)
                .onSuccess { _statusMessage.value = "Report submitted for moderation review" }
                .onFailure { _statusMessage.value = it.localizedMessage }
        }
    }

    fun loadAdminReports() {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            repo.observeAdminReports()
                .catch { e -> if (e is CancellationException) throw e }
                .collect { _adminReports.value = it }
        }
    }

    fun resolveReport(reportId: String, status: String) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            repo.resolveReport(reportId, status)
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w("OnlineVM", "Error observing $label", error)
                emit(UiState.Error(error.message ?: "Failed to load $label"))
            }

    override fun onCleared() {
        super.onCleared()
        repo.disconnectRealtime()
    }
}
