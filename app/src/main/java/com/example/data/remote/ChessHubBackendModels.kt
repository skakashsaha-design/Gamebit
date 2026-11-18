package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicProfileDoc(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val bio: String = "",
    val countryIso: String = "BD",
    val fideFederation: String = "BAN",
    val bulletRating: Long = 1500L,
    val blitzRating: Long = 1500L,
    val rapidRating: Long = 1500L,
    val classicalRating: Long = 1500L,
    val puzzleRating: Long = 1200L,
    val gamesPlayed: Long = 0L,
    val wins: Long = 0L,
    val draws: Long = 0L,
    val losses: Long = 0L,
    val studyCount: Long = 0L,
    val achievementsCount: Long = 0L,
    val onlineStatus: String = "online",
    val publicVisible: Boolean = true,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class RatingHistoryPoint(
    val id: String = "",
    val timeCategory: String = "blitz",
    val rating: Int = 1500,
    val delta: Int = 0,
    val gameId: String = "",
    val recordedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class MatchTicketDoc(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Long = 1500L,
    val variant: String = "STANDARD",
    val timeControl: String = "5+3",
    val ratedGame: Boolean = true,
    val minRating: Long = 100L,
    val maxRating: Long = 4000L,
    val status: String = "waiting",
    val matchedGameId: String = "",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class OnlineGameDoc(
    val id: String = "",
    val whiteUid: String = "",
    val blackUid: String = "",
    val whiteName: String = "",
    val blackName: String = "",
    val participantUids: List<String> = emptyList(),
    val variant: String = "STANDARD",
    val timeControl: String = "5+3",
    val initialTimeMs: Long = 300_000L,
    val incrementMs: Long = 3_000L,
    val delayMs: Long = 0L,
    val ratedGame: Boolean = true,
    val status: String = "active",
    val fen: String = "",
    val pgn: String = "",
    val lastMoveUci: String = "",
    val moveCount: Long = 0L,
    val turn: String = "w",
    val whiteTimeMs: Long = 300_000L,
    val blackTimeMs: Long = 300_000L,
    val whiteConnected: Boolean = true,
    val blackConnected: Boolean = true,
    val drawOfferBy: String = "",
    val takebackOfferBy: String = "",
    val result: String = "",
    val resultReason: String = "",
    val winnerUid: String = "",
    val serverTimestampMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class GameChatMessageDoc(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantUids: List<String> = emptyList(),
    val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class FriendshipDoc(
    val id: String = "",
    val requesterUid: String = "",
    val recipientUid: String = "",
    val memberUids: List<String> = emptyList(),
    val requesterName: String = "",
    val recipientName: String = "",
    val status: String = "pending",
    val onlineStatus: String = "offline",
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class MessageThreadDoc(
    val id: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class DirectMessageDoc(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val memberUids: List<String> = emptyList(),
    val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class CloudStudyDoc(
    val id: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val pgn: String = "",
    val tags: List<String> = emptyList(),
    val publicVisible: Boolean = false,
    val collaboratorUids: List<String> = emptyList(),
    val chapterCount: Long = 1L,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ClubDoc(
    val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val description: String = "",
    val countryIso: String = "BD",
    val memberUids: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val publicVisible: Boolean = true,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class TeamDoc(
    val id: String = "",
    val captainUid: String = "",
    val name: String = "",
    val description: String = "",
    val memberUids: List<String> = emptyList(),
    val ratingAvg: Long = 1500L,
    val publicVisible: Boolean = true,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class TournamentDoc(
    val id: String = "",
    val creatorUid: String = "",
    val name: String = "",
    val format: String = "arena",
    val variant: String = "STANDARD",
    val timeControl: String = "3+2",
    val ratedGame: Boolean = true,
    val status: String = "active",
    val round: Long = 1L,
    val maxRounds: Long = 5L,
    val participantUids: List<String> = emptyList(),
    val publicVisible: Boolean = true,
    val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class TournamentEntryDoc(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Long = 1500L,
    val points: Long = 0L,
    val streak: Long = 0L,
    val gamesPlayed: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class AbuseReportDoc(
    val id: String = "",
    val reporterUid: String = "",
    val targetUid: String = "",
    val category: String = "",
    val details: String = "",
    val status: String = "open",
    val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class RatingDeltaResult(
    val ratedGame: Boolean,
    val timeCategory: String,
    val oldRating: Int,
    val newRating: Int,
) {
    val delta: Int get() = newRating - oldRating
}

// --- REST Request & Response Envelopes ---

@JsonClass(generateAdapter = true)
data class BootstrapUserRequest(
    val username: String,
    val countryIso: String,
    val fideFederation: String,
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val username: String,
    val bio: String,
    val countryIso: String,
    val fideFederation: String,
    val publicVisible: Boolean,
)

@JsonClass(generateAdapter = true)
data class MatchmakingRequest(
    val variant: String,
    val timeControl: String,
    val initialTimeMs: Long,
    val incrementMs: Long,
    val delayMs: Long,
    val ratedGame: Boolean,
)

@JsonClass(generateAdapter = true)
data class MatchmakingResponse(
    val status: String, // "waiting" | "matched"
    val ticket: MatchTicketDoc? = null,
    val game: OnlineGameDoc? = null,
)

@JsonClass(generateAdapter = true)
data class CreateChallengeRequest(
    val opponentUid: String,
    val variant: String,
    val timeControl: String,
    val initialTimeMs: Long,
    val incrementMs: Long,
    val ratedGame: Boolean,
)

@JsonClass(generateAdapter = true)
data class SubmitMoveRequest(
    val moveUci: String,
    val expectedMoveCount: Long,
    val clientTimestampMs: Long,
)

@JsonClass(generateAdapter = true)
data class SubmitMoveResponse(
    val game: OnlineGameDoc,
    val ratingDelta: RatingDeltaResult? = null,
)

@JsonClass(generateAdapter = true)
data class GameActionResponse(
    val game: OnlineGameDoc,
    val ratingDelta: RatingDeltaResult? = null,
)

@JsonClass(generateAdapter = true)
data class GameChatRequest(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class FriendRequestBody(
    val recipientUid: String,
)

@JsonClass(generateAdapter = true)
data class FriendshipStatusRequest(
    val status: String,
)

@JsonClass(generateAdapter = true)
data class DirectMessageRequest(
    val recipientUid: String,
    val text: String,
)

@JsonClass(generateAdapter = true)
data class PublishStudyRequest(
    val title: String,
    val description: String,
    val pgn: String,
    val tags: List<String>,
    val publicVisible: Boolean,
    val chapterCount: Int,
)

@JsonClass(generateAdapter = true)
data class CreateClubRequest(
    val name: String,
    val description: String,
    val countryIso: String,
)

@JsonClass(generateAdapter = true)
data class CreateTeamRequest(
    val name: String,
    val description: String,
)

@JsonClass(generateAdapter = true)
data class CreateTournamentRequest(
    val name: String,
    val format: String,
    val variant: String,
    val timeControl: String,
    val ratedGame: Boolean,
    val maxRounds: Int,
)

@JsonClass(generateAdapter = true)
data class PresenceUpdateRequest(
    val onlineStatus: String,
    val activeGameId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AbuseReportRequest(
    val targetUid: String,
    val category: String,
    val details: String,
)

@JsonClass(generateAdapter = true)
data class ResolveReportRequest(
    val status: String,
)

@JsonClass(generateAdapter = true)
data class AdminStatusResponse(
    val isAdmin: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class IdResponse(
    val id: String,
)

// --- Real-Time WebSocket Events ---

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

sealed interface ChessHubWsEvent {
    data object Connected : ChessHubWsEvent
    data class MatchFound(val ticketId: String, val game: OnlineGameDoc) : ChessHubWsEvent
    data class GameSnapshot(val game: OnlineGameDoc) : ChessHubWsEvent
    data class MoveBroadcast(val game: OnlineGameDoc, val ratingDelta: RatingDeltaResult? = null) : ChessHubWsEvent
    data class ClockSync(
        val gameId: String,
        val whiteTimeMs: Long,
        val blackTimeMs: Long,
        val turn: String,
        val serverTimestampMs: Long,
    ) : ChessHubWsEvent
    data class GameEnded(val game: OnlineGameDoc, val ratingDelta: RatingDeltaResult? = null) : ChessHubWsEvent
    data class GameChatReceived(val gameId: String, val message: GameChatMessageDoc) : ChessHubWsEvent
    data class DirectMessageReceived(val threadId: String, val message: DirectMessageDoc) : ChessHubWsEvent
    data class PresenceChanged(val userId: String, val onlineStatus: String) : ChessHubWsEvent
    data class ServerError(val code: String, val message: String) : ChessHubWsEvent
}
