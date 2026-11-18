package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.chess.ChessBoardState
import com.example.chess.ChessVariant
import com.example.chess.Glicko2RatingSystem
import com.example.chess.GlickoRating
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class RatingDeltaResult(
    val ratedGame: Boolean,
    val timeCategory: String,
    val oldRating: Int,
    val newRating: Int,
) {
    val delta: Int get() = newRating - oldRating
}

class ChessHubOnlineRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing ChessHub Online.")

    private fun <T> observeListSafe(
        path: String,
        query: Query,
        clazz: Class<T>,
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    // --- User Bootstrap & Profile ---
    suspend fun ensureUserInitialized(
        username: String,
        countryIso: String,
        fideFederation: String,
    ): Result<Unit> = runCatchingFirestore(OperationType.WRITE, "users") {
        val uid = requireUserId()
        val safeName = username.trim().take(36).ifBlank { "Player_${uid.take(6)}" }
        val userRef = db.collection("users").document(uid)
        if (!userRef.get().await().exists()) {
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "countryIso" to countryIso,
                    "fideFederation" to fideFederation,
                    "privacyPublicProfile" to true,
                    "privacyAllowMessages" to true,
                    "privacyShowOnline" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        val pubRef = db.collection("publicProfiles").document(uid)
        if (!pubRef.get().await().exists()) {
            pubRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to safeName,
                    "bio" to "",
                    "countryIso" to countryIso,
                    "fideFederation" to fideFederation,
                    "bulletRating" to 1500L,
                    "blitzRating" to 1500L,
                    "rapidRating" to 1500L,
                    "classicalRating" to 1500L,
                    "puzzleRating" to 1200L,
                    "gamesPlayed" to 0L,
                    "wins" to 0L,
                    "draws" to 0L,
                    "losses" to 0L,
                    "studyCount" to 0L,
                    "achievementsCount" to 0L,
                    "onlineStatus" to "online",
                    "publicVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    fun observeMyProfile(uid: String): Flow<PublicProfileDoc?> = flow {
        requireUserId()
        val path = "publicProfiles/$uid"
        emitAll(
            db.collection("publicProfiles").document(uid)
                .snapshots()
                .map { it.toObject(PublicProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                },
        )
    }

    fun observeLeaderboard(): Flow<List<PublicProfileDoc>> = observeListSafe(
        path = "publicProfiles",
        query = db.collection("publicProfiles").whereEqualTo("publicVisible", true).limit(50),
        clazz = PublicProfileDoc::class.java,
    )

    suspend fun searchPublicPlayers(queryText: String): Result<List<PublicProfileDoc>> =
        runCatchingFirestore(OperationType.LIST, "publicProfiles") {
            requireUserId()
            val clean = queryText.trim().lowercase()
            val snap = db.collection("publicProfiles")
                .whereEqualTo("publicVisible", true)
                .limit(100)
                .get()
                .await()
            val all = snap.toObjects(PublicProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (clean.isBlank()) all.sortedByDescending { it.blitzRating }
            else all.filter {
                it.username.lowercase().contains(clean) ||
                    it.countryIso.lowercase() == clean ||
                    it.fideFederation.lowercase() == clean
            }.sortedByDescending { it.blitzRating }
        }

    suspend fun fetchOpponentRating(opponentUid: String, timeCategory: String): Double {
        return try {
            if (opponentUid.isBlank()) return 1500.0
            val doc = db.collection("publicProfiles").document(opponentUid).get().await()
            val prof = doc.toObject(PublicProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            when (timeCategory) {
                "bullet" -> prof?.bulletRating ?: 1500L
                "blitz" -> prof?.blitzRating ?: 1500L
                "rapid" -> prof?.rapidRating ?: 1500L
                else -> prof?.classicalRating ?: 1500L
            }.toDouble()
        } catch (_: Exception) {
            1500.0
        }
    }

    suspend fun updateProfileIdentity(
        username: String,
        bio: String,
        countryIso: String,
        fideFederation: String,
        publicVisible: Boolean,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return runCatchingFirestore(OperationType.UPDATE, "publicProfiles/$uid") {
            val verifiedUid = requireUserId()
            val cleanName = username.trim().take(36)
            require(cleanName.length >= 2) { "Username must be at least 2 characters." }
            db.collection("publicProfiles").document(verifiedUid).update(
                mapOf(
                    "username" to cleanName,
                    "bio" to bio.trim().take(240),
                    "countryIso" to countryIso,
                    "fideFederation" to fideFederation,
                    "publicVisible" to publicVisible,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            db.collection("users").document(verifiedUid).update(
                mapOf(
                    "displayName" to cleanName,
                    "countryIso" to countryIso,
                    "fideFederation" to fideFederation,
                    "privacyPublicProfile" to publicVisible,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    // --- Matchmaking & Online Games ---
    fun observeMyGames(uid: String): Flow<List<OnlineGameDoc>> = observeListSafe(
        path = "games",
        query = db.collection("games").whereArrayContains("participantUids", uid).limit(30),
        clazz = OnlineGameDoc::class.java,
    )

    fun observeLiveWatchGames(): Flow<List<OnlineGameDoc>> = observeListSafe(
        path = "games",
        query = db.collection("games").whereEqualTo("status", "active").limit(20),
        clazz = OnlineGameDoc::class.java,
    )

    fun observeGame(gameId: String): Flow<OnlineGameDoc?> = flow {
        val uid = requireUserId()
        val path = "games/$gameId"
        emitAll(
            db.collection("games").document(gameId)
                .snapshots()
                .map { it.toObject(OnlineGameDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                },
        )
    }

    suspend fun findOrCreateMatch(
        username: String,
        rating: Int,
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        delayMs: Long,
        ratedGame: Boolean,
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, "matchmaking") {
        val uid = requireUserId()
        val waitingSnap = db.collection("matchmaking")
            .whereEqualTo("status", "waiting")
            .limit(20)
            .get()
            .await()

        val compatible = waitingSnap.toObjects(MatchTicketDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .firstOrNull { ticket ->
                ticket.userId != uid &&
                    ticket.variant == variant.name &&
                    ticket.timeControl == timeControl &&
                    ticket.ratedGame == ratedGame &&
                    rating.toLong() in ticket.minRating..ticket.maxRating
            }

        if (compatible != null) {
            val gameId = "game_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val initialFen = ChessBoardState.initialForVariant(variant).toFen()
            db.collection("games").document(gameId).set(
                mapOf(
                    "whiteUid" to compatible.userId,
                    "blackUid" to uid,
                    "whiteName" to compatible.username,
                    "blackName" to username,
                    "participantUids" to listOf(compatible.userId, uid),
                    "variant" to variant.name,
                    "timeControl" to timeControl,
                    "initialTimeMs" to initialTimeMs,
                    "incrementMs" to incrementMs,
                    "delayMs" to delayMs,
                    "ratedGame" to ratedGame,
                    "status" to "active",
                    "fen" to initialFen,
                    "pgn" to "",
                    "lastMoveUci" to "",
                    "moveCount" to 0L,
                    "turn" to "w",
                    "whiteTimeMs" to initialTimeMs,
                    "blackTimeMs" to initialTimeMs,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            db.collection("matchmaking").document(compatible.id).update(
                mapOf(
                    "status" to "matched",
                    "matchedGameId" to gameId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            gameId
        } else {
            val ticketId = "tkt_$uid"
            db.collection("matchmaking").document(ticketId).set(
                mapOf(
                    "userId" to uid,
                    "username" to username,
                    "rating" to rating.toLong(),
                    "variant" to variant.name,
                    "timeControl" to timeControl,
                    "ratedGame" to ratedGame,
                    "minRating" to (rating - 350).coerceAtLeast(100).toLong(),
                    "maxRating" to (rating + 350).coerceAtMost(4000).toLong(),
                    "status" to "waiting",
                    "matchedGameId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            "WAITING:$ticketId"
        }
    }

    fun observeMatchTicket(ticketId: String): Flow<MatchTicketDoc?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("matchmaking").document(ticketId)
                .snapshots()
                .map { it.toObject(MatchTicketDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, "matchmaking/$ticketId")
                    throw error
                },
        )
    }

    suspend fun cancelMatchTicket(ticketId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "matchmaking/$ticketId") {
            requireUserId()
            db.collection("matchmaking").document(ticketId).delete().await()
        }

    suspend fun createFriendChallengeGame(
        friendUid: String,
        friendName: String,
        myName: String,
        variant: ChessVariant,
        timeControl: String,
        initialTimeMs: Long,
        incrementMs: Long,
        ratedGame: Boolean,
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "games") {
        val uid = requireUserId()
        val gameId = "game_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val initialFen = ChessBoardState.initialForVariant(variant).toFen()
        db.collection("games").document(gameId).set(
            mapOf(
                "whiteUid" to uid,
                "blackUid" to friendUid,
                "whiteName" to myName,
                "blackName" to friendName,
                "participantUids" to listOf(uid, friendUid),
                "variant" to variant.name,
                "timeControl" to timeControl,
                "initialTimeMs" to initialTimeMs,
                "incrementMs" to incrementMs,
                "delayMs" to 0L,
                "ratedGame" to ratedGame,
                "status" to "active",
                "fen" to initialFen,
                "pgn" to "",
                "lastMoveUci" to "",
                "moveCount" to 0L,
                "turn" to "w",
                "whiteTimeMs" to initialTimeMs,
                "blackTimeMs" to initialTimeMs,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        gameId
    }

    suspend fun submitOnlineMove(
        gameId: String,
        newFen: String,
        newPgn: String,
        lastMoveUci: String,
        moveCount: Long,
        nextTurn: String,
        whiteTimeMs: Long,
        blackTimeMs: Long,
        terminalStatus: String? = null,
        result: String? = null,
        resultReason: String? = null,
        winnerUid: String? = null,
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "games/$gameId") {
        requireUserId()
        val payload = linkedMapOf<String, Any>(
            "fen" to newFen,
            "pgn" to newPgn,
            "lastMoveUci" to lastMoveUci,
            "moveCount" to moveCount,
            "turn" to nextTurn,
            "whiteTimeMs" to whiteTimeMs.coerceAtLeast(0L),
            "blackTimeMs" to blackTimeMs.coerceAtLeast(0L),
            "drawOfferBy" to "",
            "takebackOfferBy" to "",
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (terminalStatus != null) {
            payload["status"] = terminalStatus
            result?.let { payload["result"] = it }
            resultReason?.let { payload["resultReason"] = it }
            winnerUid?.let { payload["winnerUid"] = it }
        }
        db.collection("games").document(gameId).update(payload).await()
    }

    suspend fun finishOnlineGame(
        gameId: String,
        status: String,
        result: String,
        reason: String,
        winnerUid: String,
        ratedGame: Boolean,
        timeCategory: String,
        opponentRating: Double,
        myScore: Double,
    ): Result<RatingDeltaResult?> = runCatchingFirestore(OperationType.UPDATE, "games/$gameId") {
        val uid = requireUserId()
        db.collection("games").document(gameId).update(
            mapOf(
                "status" to status,
                "result" to result,
                "resultReason" to reason,
                "winnerUid" to winnerUid,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()

        var deltaSummary: RatingDeltaResult? = null
        if (ratedGame && status == "finished") {
            val profileRef = db.collection("publicProfiles").document(uid)
            val snap = profileRef.get().await()
            val prof = snap.toObject(PublicProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (prof != null) {
                val fieldName = when (timeCategory) {
                    "bullet" -> "bulletRating"
                    "blitz" -> "blitzRating"
                    "rapid" -> "rapidRating"
                    else -> "classicalRating"
                }
                val currentR = when (timeCategory) {
                    "bullet" -> prof.bulletRating
                    "blitz" -> prof.blitzRating
                    "rapid" -> prof.rapidRating
                    else -> prof.classicalRating
                }.toDouble()
                val updated = Glicko2RatingSystem.updateSingleGame(
                    player = GlickoRating(currentR, 120.0, 0.06),
                    opponent = GlickoRating(opponentRating, 120.0, 0.06),
                    score = myScore,
                )
                profileRef.update(
                    mapOf(
                        fieldName to updated.displayInt.toLong(),
                        "gamesPlayed" to (prof.gamesPlayed + 1L),
                        "wins" to (prof.wins + if (myScore == 1.0) 1L else 0L),
                        "draws" to (prof.draws + if (myScore == 0.5) 1L else 0L),
                        "losses" to (prof.losses + if (myScore == 0.0) 1L else 0L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                deltaSummary = RatingDeltaResult(
                    ratedGame = true,
                    timeCategory = timeCategory,
                    oldRating = currentR.toInt(),
                    newRating = updated.displayInt,
                )
            }
        } else {
            deltaSummary = RatingDeltaResult(
                ratedGame = false,
                timeCategory = timeCategory,
                oldRating = 0,
                newRating = 0,
            )
        }
        deltaSummary
    }

    suspend fun offerDraw(gameId: String): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "games/$gameId") {
        val uid = requireUserId()
        db.collection("games").document(gameId).update(
            mapOf("drawOfferBy" to uid, "updatedAt" to FieldValue.serverTimestamp()),
        ).await()
    }

    // --- Game Chat ---
    fun observeGameChat(gameId: String): Flow<List<GameChatMessageDoc>> = observeListSafe(
        path = "games/$gameId/chat",
        query = db.collection("games").document(gameId).collection("chat").orderBy("createdAt", Query.Direction.ASCENDING).limit(50),
        clazz = GameChatMessageDoc::class.java,
    )

    suspend fun sendGameChat(
        gameId: String,
        senderName: String,
        text: String,
        participantUids: List<String>,
    ): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "games/$gameId/chat") {
        val uid = requireUserId()
        val clean = text.trim().take(300)
        require(clean.isNotEmpty()) { "Message cannot be empty." }
        db.collection("games").document(gameId).collection("chat").document().set(
            mapOf(
                "senderUid" to uid,
                "senderName" to senderName.take(50),
                "text" to clean,
                "participantUids" to participantUids,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    // --- Friends & Messaging ---
    fun observeFriendships(uid: String): Flow<List<FriendshipDoc>> = observeListSafe(
        path = "friends",
        query = db.collection("friends").whereArrayContains("memberUids", uid),
        clazz = FriendshipDoc::class.java,
    )

    suspend fun sendFriendRequest(
        recipientUid: String,
        recipientName: String,
        myName: String,
    ): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "friends") {
        val uid = requireUserId()
        require(recipientUid != uid) { "Cannot add yourself." }
        val sorted = listOf(uid, recipientUid).sorted()
        val docId = "${sorted[0]}_${sorted[1]}"
        db.collection("friends").document(docId).set(
            mapOf(
                "requesterUid" to uid,
                "recipientUid" to recipientUid,
                "memberUids" to sorted,
                "requesterName" to myName.take(50),
                "recipientName" to recipientName.take(50),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun updateFriendshipStatus(friendshipId: String, status: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "friends/$friendshipId") {
            requireUserId()
            db.collection("friends").document(friendshipId).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()),
            ).await()
        }

    fun observeMessageThreads(uid: String): Flow<List<MessageThreadDoc>> = observeListSafe(
        path = "messages",
        query = db.collection("messages").whereArrayContains("memberUids", uid),
        clazz = MessageThreadDoc::class.java,
    )

    fun observeDirectMessages(threadId: String): Flow<List<DirectMessageDoc>> = observeListSafe(
        path = "messages/$threadId/items",
        query = db.collection("messages").document(threadId).collection("items")
            .orderBy("createdAt", Query.Direction.ASCENDING).limit(50),
        clazz = DirectMessageDoc::class.java,
    )

    suspend fun sendDirectMessage(
        recipientUid: String,
        senderName: String,
        text: String,
    ): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "messages") {
        val uid = requireUserId()
        val clean = text.trim().take(1000)
        require(clean.isNotEmpty()) { "Message cannot be empty." }
        val sorted = listOf(uid, recipientUid).sorted()
        val threadId = "${sorted[0]}_${sorted[1]}"
        val threadRef = db.collection("messages").document(threadId)
        val snap = threadRef.get().await()
        if (!snap.exists()) {
            threadRef.set(
                mapOf(
                    "memberUids" to sorted,
                    "lastMessage" to clean.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } else {
            threadRef.update(
                mapOf(
                    "lastMessage" to clean.take(300),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        threadRef.collection("items").document().set(
            mapOf(
                "senderUid" to uid,
                "senderName" to senderName.take(50),
                "text" to clean,
                "memberUids" to sorted,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    // --- Cloud Studies ---
    fun observeMyStudies(uid: String): Flow<List<CloudStudyDoc>> = observeListSafe(
        path = "studies",
        query = db.collection("studies").whereEqualTo("ownerUid", uid).limit(40),
        clazz = CloudStudyDoc::class.java,
    )

    fun observePublicStudies(): Flow<List<CloudStudyDoc>> = observeListSafe(
        path = "studies",
        query = db.collection("studies").whereEqualTo("publicVisible", true).limit(30),
        clazz = CloudStudyDoc::class.java,
    )

    suspend fun publishCloudStudy(
        ownerName: String,
        title: String,
        description: String,
        pgn: String,
        tags: List<String>,
        publicVisible: Boolean,
        chapterCount: Int,
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "studies") {
        val uid = requireUserId()
        val docRef = db.collection("studies").document()
        docRef.set(
            mapOf(
                "ownerUid" to uid,
                "ownerName" to ownerName.take(50),
                "title" to title.trim().take(120).ifBlank { "Untitled Study" },
                "description" to description.trim().take(1000),
                "pgn" to pgn.take(90_000),
                "tags" to tags.take(10),
                "publicVisible" to publicVisible,
                "collaboratorUids" to emptyList<String>(),
                "chapterCount" to chapterCount.coerceIn(1, 64).toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        docRef.id
    }

    // --- Clubs, Teams, Tournaments ---
    fun observeClubs(): Flow<List<ClubDoc>> = observeListSafe(
        path = "clubs",
        query = db.collection("clubs").whereEqualTo("publicVisible", true).limit(30),
        clazz = ClubDoc::class.java,
    )

    suspend fun createClub(name: String, description: String, countryIso: String): Result<Unit> =
        runCatchingFirestore(OperationType.CREATE, "clubs") {
            val uid = requireUserId()
            db.collection("clubs").document().set(
                mapOf(
                    "ownerUid" to uid,
                    "name" to name.trim().take(80),
                    "description" to description.trim().take(500),
                    "countryIso" to countryIso,
                    "memberUids" to listOf(uid),
                    "memberCount" to 1L,
                    "publicVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    suspend fun joinClub(club: ClubDoc): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "clubs/${club.id}") {
            val uid = requireUserId()
            if (uid in club.memberUids) return@runCatchingFirestore
            val updatedMembers = (club.memberUids + uid).distinct()
            db.collection("clubs").document(club.id).update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "memberCount" to updatedMembers.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    fun observeTeams(): Flow<List<TeamDoc>> = observeListSafe(
        path = "teams",
        query = db.collection("teams").whereEqualTo("publicVisible", true).limit(30),
        clazz = TeamDoc::class.java,
    )

    suspend fun createTeam(name: String, description: String): Result<Unit> =
        runCatchingFirestore(OperationType.CREATE, "teams") {
            val uid = requireUserId()
            db.collection("teams").document().set(
                mapOf(
                    "captainUid" to uid,
                    "name" to name.trim().take(80),
                    "description" to description.trim().take(500),
                    "memberUids" to listOf(uid),
                    "ratingAvg" to 1500L,
                    "publicVisible" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    fun observeTournaments(): Flow<List<TournamentDoc>> = observeListSafe(
        path = "tournaments",
        query = db.collection("tournaments").whereEqualTo("publicVisible", true).limit(30),
        clazz = TournamentDoc::class.java,
    )

    fun observeTournamentEntries(tournamentId: String): Flow<List<TournamentEntryDoc>> = observeListSafe(
        path = "tournaments/$tournamentId/entries",
        query = db.collection("tournaments").document(tournamentId).collection("entries")
            .orderBy("points", Query.Direction.DESCENDING).limit(50),
        clazz = TournamentEntryDoc::class.java,
    )

    suspend fun createTournament(
        name: String,
        format: String,
        variant: String,
        timeControl: String,
        ratedGame: Boolean,
        maxRounds: Int,
        creatorName: String,
        creatorRating: Int,
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "tournaments") {
        val uid = requireUserId()
        val ref = db.collection("tournaments").document()
        ref.set(
            mapOf(
                "creatorUid" to uid,
                "name" to name.trim().take(80),
                "format" to format,
                "variant" to variant,
                "timeControl" to timeControl,
                "ratedGame" to ratedGame,
                "status" to "active",
                "round" to 1L,
                "maxRounds" to maxRounds.coerceIn(1, 20).toLong(),
                "participantUids" to listOf(uid),
                "publicVisible" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        ref.collection("entries").document(uid).set(
            mapOf(
                "userId" to uid,
                "username" to creatorName.take(40),
                "rating" to creatorRating.toLong(),
                "points" to 0L,
                "streak" to 0L,
                "gamesPlayed" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        ref.id
    }

    suspend fun joinTournament(tournament: TournamentDoc, username: String, rating: Int): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "tournaments/${tournament.id}") {
            val uid = requireUserId()
            if (uid !in tournament.participantUids) {
                db.collection("tournaments").document(tournament.id).update(
                    mapOf(
                        "participantUids" to (tournament.participantUids + uid).distinct(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            db.collection("tournaments").document(tournament.id).collection("entries").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "username" to username.take(40),
                    "rating" to rating.toLong(),
                    "points" to 0L,
                    "streak" to 0L,
                    "gamesPlayed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    // --- Moderation & Admin ---
    suspend fun submitAbuseReport(targetUid: String, category: String, details: String): Result<Unit> =
        runCatchingFirestore(OperationType.CREATE, "reports") {
            val uid = requireUserId()
            require(details.trim().length >= 4) { "Please provide at least 4 characters of details." }
            db.collection("reports").document().set(
                mapOf(
                    "reporterUid" to uid,
                    "targetUid" to targetUid.ifBlank { "general" },
                    "category" to category.take(50),
                    "details" to details.trim().take(1000),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    suspend fun checkIsAdmin(): Boolean = try {
        val uid = requireUserId()
        db.collection("admins").document(uid).get().await().exists()
    } catch (_: Exception) {
        false
    }

    fun observeAdminReports(): Flow<List<AbuseReportDoc>> = observeListSafe(
        path = "reports",
        query = db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING).limit(40),
        clazz = AbuseReportDoc::class.java,
    )

    suspend fun resolveReport(reportId: String, status: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "reports/$reportId") {
            requireUserId()
            db.collection("reports").document(reportId).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()),
            ).await()
        }

    private suspend inline fun <T> runCatchingFirestore(
        op: OperationType,
        path: String,
        block: () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}
