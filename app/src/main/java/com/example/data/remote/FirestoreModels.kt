package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserPrivateDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val countryIso: String = "BD",
    val fideFederation: String = "BAN",
    val privacyPublicProfile: Boolean = true,
    val privacyAllowMessages: Boolean = true,
    val privacyShowOnline: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PublicProfileDoc(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MatchTicketDoc(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OnlineGameDoc(
    @DocumentId val id: String = "",
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
    val drawOfferBy: String = "",
    val takebackOfferBy: String = "",
    val result: String = "",
    val resultReason: String = "",
    val winnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GameChatMessageDoc(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendshipDoc(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val recipientUid: String = "",
    val memberUids: List<String> = emptyList(),
    val requesterName: String = "",
    val recipientName: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MessageThreadDoc(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DirectMessageDoc(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudStudyDoc(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val pgn: String = "",
    val tags: List<String> = emptyList(),
    val publicVisible: Boolean = false,
    val collaboratorUids: List<String> = emptyList(),
    val chapterCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ClubDoc(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val description: String = "",
    val countryIso: String = "BD",
    val memberUids: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ClubMessageDoc(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TeamDoc(
    @DocumentId val id: String = "",
    val captainUid: String = "",
    val name: String = "",
    val description: String = "",
    val memberUids: List<String> = emptyList(),
    val ratingAvg: Long = 1500L,
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TournamentDoc(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TournamentEntryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Long = 1500L,
    val points: Long = 0L,
    val streak: Long = 0L,
    val gamesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AbuseReportDoc(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetUid: String = "",
    val category: String = "",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)
