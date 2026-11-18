package com.example.data.remote

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Modular REST/HTTPS contract for the ChessHub backend server.
 * All matchmaking, move validation, clocks, rating calculation, social, and tournament
 * endpoints are server-authoritative and easily swappable behind [OnlineChessRepository].
 */
interface ChessHubBackendApi {

    // --- Profile & Rankings ---
    @POST("v1/users/bootstrap")
    suspend fun bootstrapUser(@Body body: BootstrapUserRequest): PublicProfileDoc

    @GET("v1/profiles/me")
    suspend fun getMyProfile(): PublicProfileDoc

    @PUT("v1/profiles/me")
    suspend fun updateMyProfile(@Body body: UpdateProfileRequest): PublicProfileDoc

    @GET("v1/profiles/{userId}")
    suspend fun getPlayerProfile(@Path("userId") userId: String): PublicProfileDoc

    @GET("v1/profiles/{userId}/rating-history")
    suspend fun getRatingHistory(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 40,
    ): List<RatingHistoryPoint>

    @GET("v1/profiles/search")
    suspend fun searchPlayers(@Query("q") query: String): List<PublicProfileDoc>

    @GET("v1/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 50): List<PublicProfileDoc>

    // --- Matchmaking & Real-Time Chess Games (Server-Authoritative) ---
    @POST("v1/matchmaking/tickets")
    suspend fun createOrJoinMatchmaking(@Body body: MatchmakingRequest): MatchmakingResponse

    @GET("v1/matchmaking/tickets/{ticketId}")
    suspend fun getMatchTicket(@Path("ticketId") ticketId: String): MatchTicketDoc

    @DELETE("v1/matchmaking/tickets/{ticketId}")
    suspend fun cancelMatchTicket(@Path("ticketId") ticketId: String)

    @GET("v1/games/mine")
    suspend fun getMyGames(@Query("limit") limit: Int = 30): List<OnlineGameDoc>

    @GET("v1/games/live")
    suspend fun getLiveWatchGames(@Query("limit") limit: Int = 20): List<OnlineGameDoc>

    @GET("v1/games/{gameId}")
    suspend fun getGame(@Path("gameId") gameId: String): OnlineGameDoc

    @POST("v1/games/challenge")
    suspend fun createChallengeGame(@Body body: CreateChallengeRequest): OnlineGameDoc

    @POST("v1/games/{gameId}/moves")
    suspend fun submitMove(
        @Path("gameId") gameId: String,
        @Body body: SubmitMoveRequest,
    ): SubmitMoveResponse

    @POST("v1/games/{gameId}/resign")
    suspend fun resignGame(@Path("gameId") gameId: String): GameActionResponse

    @POST("v1/games/{gameId}/draw")
    suspend fun offerOrAcceptDraw(@Path("gameId") gameId: String): GameActionResponse

    @GET("v1/games/{gameId}/chat")
    suspend fun getGameChat(
        @Path("gameId") gameId: String,
        @Query("limit") limit: Int = 50,
    ): List<GameChatMessageDoc>

    @POST("v1/games/{gameId}/chat")
    suspend fun postGameChat(
        @Path("gameId") gameId: String,
        @Body body: GameChatRequest,
    ): GameChatMessageDoc

    // --- Friends & Direct Messaging ---
    @GET("v1/friends")
    suspend fun getFriendships(): List<FriendshipDoc>

    @POST("v1/friends")
    suspend fun sendFriendRequest(@Body body: FriendRequestBody): FriendshipDoc

    @PATCH("v1/friends/{friendshipId}")
    suspend fun updateFriendshipStatus(
        @Path("friendshipId") friendshipId: String,
        @Body body: FriendshipStatusRequest,
    ): FriendshipDoc

    @GET("v1/messages/threads")
    suspend fun getMessageThreads(): List<MessageThreadDoc>

    @GET("v1/messages/threads/{threadId}")
    suspend fun getDirectMessages(
        @Path("threadId") threadId: String,
        @Query("limit") limit: Int = 50,
    ): List<DirectMessageDoc>

    @POST("v1/messages/send")
    suspend fun sendDirectMessage(@Body body: DirectMessageRequest): DirectMessageDoc

    // --- Cloud Studies, Clubs, Teams, Tournaments ---
    @GET("v1/studies/public")
    suspend fun getPublicStudies(@Query("limit") limit: Int = 30): List<CloudStudyDoc>

    @GET("v1/studies/mine")
    suspend fun getMyStudies(@Query("limit") limit: Int = 40): List<CloudStudyDoc>

    @POST("v1/studies")
    suspend fun publishStudy(@Body body: PublishStudyRequest): IdResponse

    @GET("v1/clubs")
    suspend fun getClubs(@Query("limit") limit: Int = 30): List<ClubDoc>

    @POST("v1/clubs")
    suspend fun createClub(@Body body: CreateClubRequest): ClubDoc

    @POST("v1/clubs/{clubId}/join")
    suspend fun joinClub(@Path("clubId") clubId: String): ClubDoc

    @GET("v1/teams")
    suspend fun getTeams(@Query("limit") limit: Int = 30): List<TeamDoc>

    @POST("v1/teams")
    suspend fun createTeam(@Body body: CreateTeamRequest): TeamDoc

    @GET("v1/tournaments")
    suspend fun getTournaments(@Query("limit") limit: Int = 30): List<TournamentDoc>

    @GET("v1/tournaments/{tournamentId}/standings")
    suspend fun getTournamentStandings(
        @Path("tournamentId") tournamentId: String,
        @Query("limit") limit: Int = 50,
    ): List<TournamentEntryDoc>

    @POST("v1/tournaments")
    suspend fun createTournament(@Body body: CreateTournamentRequest): TournamentDoc

    @POST("v1/tournaments/{tournamentId}/join")
    suspend fun joinTournament(@Path("tournamentId") tournamentId: String): TournamentDoc

    // --- Presence & Moderation ---
    @POST("v1/presence")
    suspend fun updatePresence(@Body body: PresenceUpdateRequest)

    @POST("v1/reports")
    suspend fun submitReport(@Body body: AbuseReportRequest): IdResponse

    @GET("v1/admin/status")
    suspend fun getAdminStatus(): AdminStatusResponse

    @GET("v1/admin/reports")
    suspend fun getAdminReports(@Query("limit") limit: Int = 40): List<AbuseReportDoc>

    @PATCH("v1/admin/reports/{reportId}")
    suspend fun resolveReport(
        @Path("reportId") reportId: String,
        @Body body: ResolveReportRequest,
    ): AbuseReportDoc
}

object ChessHubApiClientFactory {
    fun createMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(accessTokenProvider: () -> String): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = accessTokenProvider()
            val original: Request = chain.request()
            val req = original.newBuilder().apply {
                header("Accept", "application/json")
                header("User-Agent", "ChessHub-Android/2.0")
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }.build()
            chain.proceed(req)
        }
        .build()

    fun createApi(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        moshi: Moshi = createMoshi(),
    ): ChessHubBackendApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChessHubBackendApi::class.java)
    }
}

@JsonClass(generateAdapter = true)
internal data class BackendErrorEnvelope(
    val message: String? = null,
    val error: String? = null,
)

object BackendErrorHandler {
    private const val TAG = "ChessHubBackend"
    private val errorAdapter = ChessHubApiClientFactory.createMoshi().adapter(BackendErrorEnvelope::class.java)

    fun mapError(error: Throwable, operation: String, serverUrl: String): Throwable {
        if (error is CancellationException) return error
        val message = when (error) {
            is HttpException -> {
                val rawBody = runCatching { error.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
                val serverMsg = runCatching {
                    val env = errorAdapter.fromJson(rawBody)
                    env?.message?.takeIf { it.isNotBlank() } ?: env?.error.orEmpty()
                }.getOrDefault("")
                if (serverMsg.isNotBlank()) serverMsg else "Server returned HTTP ${error.code()} during $operation"
            }
            is IOException -> {
                "Cannot reach ChessHub server at $serverUrl (${error.localizedMessage ?: "network error"})."
            }
            else -> error.localizedMessage ?: "Backend error during $operation"
        }
        Log.w(TAG, "$operation failed: $message")
        return IllegalStateException(message, error)
    }
}
