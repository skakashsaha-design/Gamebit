package com.example.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.ChessHubApiClientFactory
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
internal data class AuthLoginPayload(
    val identifier: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
internal data class AuthRegisterPayload(
    val username: String,
    val email: String,
    val password: String,
    val countryIso: String,
    val fideFederation: String,
)

@JsonClass(generateAdapter = true)
internal data class AuthUserPayload(
    val userId: String? = null,
    val uid: String? = null,
    val username: String? = null,
    val email: String? = null,
    val countryIso: String? = null,
    val fideFederation: String? = null,
    val isAdmin: Boolean? = null,
)

@JsonClass(generateAdapter = true)
internal data class AuthSessionEnvelope(
    val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMs: Long? = null,
    val user: AuthUserPayload? = null,
    val userId: String? = null,
    val uid: String? = null,
    val username: String? = null,
    val email: String? = null,
    val countryIso: String? = null,
    val fideFederation: String? = null,
    val isAdmin: Boolean? = null,
)

@JsonClass(generateAdapter = true)
internal data class AuthErrorEnvelope(
    val message: String? = null,
    val error: String? = null,
)

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "chesshub_auth_session")

data class AuthUser(
    val uid: String,
    val username: String,
    val email: String? = null,
    val countryIso: String = "BD",
    val fideFederation: String = "BAN",
    val isAdmin: Boolean = false,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val user: AuthUser,
    val serverUrl: String,
    val expiresAtEpochMs: Long = 0L,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val session: AuthSession) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val serverBaseUrl: StateFlow<String>

    suspend fun updateServerUrl(url: String): String
    suspend fun signInWithCredentials(
        serverUrl: String,
        usernameOrEmail: String,
        password: String,
    ): Result<AuthSession>

    suspend fun registerAccount(
        serverUrl: String,
        username: String,
        email: String,
        password: String,
        countryIso: String,
        fideFederation: String,
    ): Result<AuthSession>

    suspend fun signInWithToken(
        serverUrl: String,
        accessToken: String,
    ): Result<AuthSession>

    suspend fun signOut()

    companion object {
        const val DEFAULT_CHESSHUB_SERVER_URL = "https://api.chesshub.org"

        fun normalizeServerUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isBlank()) return DEFAULT_CHESSHUB_SERVER_URL
            return when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed
                trimmed.startsWith("localhost", ignoreCase = true) ||
                    trimmed.startsWith("10.0.2.2") ||
                    trimmed.startsWith("127.0.0.1") -> "http://$trimmed"
                else -> "https://$trimmed"
            }
        }
    }
}

class HttpAuthRepository(
    context: Context,
    private val okHttpClient: OkHttpClient = defaultAuthOkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val COUNTRY_ISO = stringPreferencesKey("country_iso")
        val FIDE_FED = stringPreferencesKey("fide_fed")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val SERVER_URL = stringPreferencesKey("server_url")
        val EXPIRES_AT = longPreferencesKey("expires_at")
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _serverBaseUrl = MutableStateFlow(AuthRepository.DEFAULT_CHESSHUB_SERVER_URL)
    override val serverBaseUrl: StateFlow<String> = _serverBaseUrl.asStateFlow()

    init {
        scope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        val prefs = appContext.authDataStore.data.first()
        val storedUrl = prefs[Keys.SERVER_URL]?.takeIf { it.isNotBlank() }
            ?: AuthRepository.DEFAULT_CHESSHUB_SERVER_URL
        _serverBaseUrl.value = storedUrl

        val token = prefs[Keys.ACCESS_TOKEN].orEmpty()
        val uid = prefs[Keys.USER_ID].orEmpty()
        val expiresAt = prefs[Keys.EXPIRES_AT] ?: 0L
        val isExpired = expiresAt > 0L && System.currentTimeMillis() >= expiresAt

        if (token.isNotBlank() && uid.isNotBlank() && !isExpired) {
            val session = AuthSession(
                accessToken = token,
                refreshToken = prefs[Keys.REFRESH_TOKEN],
                user = AuthUser(
                    uid = uid,
                    username = prefs[Keys.USERNAME].orEmpty().ifBlank { "Player" },
                    email = prefs[Keys.EMAIL],
                    countryIso = prefs[Keys.COUNTRY_ISO].orEmpty().ifBlank { "BD" },
                    fideFederation = prefs[Keys.FIDE_FED].orEmpty().ifBlank { "BAN" },
                    isAdmin = prefs[Keys.IS_ADMIN] ?: false,
                ),
                serverUrl = storedUrl,
                expiresAtEpochMs = expiresAt,
            )
            _authState.value = AuthState.Authenticated(session)
        } else {
            if (isExpired) clearStoredSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun updateServerUrl(url: String): String {
        val normalized = AuthRepository.normalizeServerUrl(url)
        _serverBaseUrl.value = normalized
        appContext.authDataStore.edit { it[Keys.SERVER_URL] = normalized }
        return normalized
    }

    override suspend fun signInWithCredentials(
        serverUrl: String,
        usernameOrEmail: String,
        password: String,
    ): Result<AuthSession> = withContext(ioDispatcher) {
        runCatching {
            val normalizedUrl = updateServerUrl(serverUrl)
            val identifier = usernameOrEmail.trim()
            require(identifier.length >= 2) { "Enter your username or email." }
            require(password.length >= 6) { "Password must be at least 6 characters." }

            val payloadJson = authMoshi.adapter(AuthLoginPayload::class.java).toJson(
                AuthLoginPayload(identifier = identifier, password = password),
            )
            val session = executeAuthPost("$normalizedUrl/v1/auth/login", payloadJson, normalizedUrl)
            persistSession(session)
            _authState.value = AuthState.Authenticated(session)
            session
        }
    }

    override suspend fun registerAccount(
        serverUrl: String,
        username: String,
        email: String,
        password: String,
        countryIso: String,
        fideFederation: String,
    ): Result<AuthSession> = withContext(ioDispatcher) {
        runCatching {
            val normalizedUrl = updateServerUrl(serverUrl)
            val cleanUser = username.trim()
            val cleanEmail = email.trim()
            require(cleanUser.length >= 2) { "Username must be at least 2 characters." }
            require(cleanEmail.contains("@")) { "Enter a valid email address." }
            require(password.length >= 6) { "Password must be at least 6 characters." }

            val payloadJson = authMoshi.adapter(AuthRegisterPayload::class.java).toJson(
                AuthRegisterPayload(
                    username = cleanUser,
                    email = cleanEmail,
                    password = password,
                    countryIso = countryIso,
                    fideFederation = fideFederation,
                ),
            )
            val session = executeAuthPost("$normalizedUrl/v1/auth/register", payloadJson, normalizedUrl)
            persistSession(session)
            _authState.value = AuthState.Authenticated(session)
            session
        }
    }

    override suspend fun signInWithToken(
        serverUrl: String,
        accessToken: String,
    ): Result<AuthSession> = withContext(ioDispatcher) {
        runCatching {
            val normalizedUrl = updateServerUrl(serverUrl)
            val cleanToken = accessToken.trim()
            require(cleanToken.isNotBlank()) { "Access token cannot be empty." }

            val req = Request.Builder()
                .url("$normalizedUrl/v1/auth/me")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/json")
                .get()
                .build()

            val body = try {
                okHttpClient.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(parseServerError(raw, resp.code, normalizedUrl))
                    }
                    raw
                }
            } catch (io: IOException) {
                throw IllegalStateException(
                    "Unable to reach ChessHub server at $normalizedUrl (${io.localizedMessage ?: "network error"}). Check your connection or Continue Offline.",
                    io,
                )
            }

            val env = authMoshi.adapter(AuthSessionEnvelope::class.java).fromJson(body)
                ?: error("Empty profile response from server.")
            val uid = env.user?.userId?.takeIf { it.isNotBlank() }
                ?: env.user?.uid?.takeIf { it.isNotBlank() }
                ?: env.userId?.takeIf { it.isNotBlank() }
                ?: env.uid?.takeIf { it.isNotBlank() }
                ?: error("Missing user id in server response.")
            val session = AuthSession(
                accessToken = cleanToken,
                refreshToken = env.refreshToken?.takeIf { it.isNotBlank() },
                user = AuthUser(
                    uid = uid,
                    username = env.user?.username ?: env.username ?: "Player",
                    email = (env.user?.email ?: env.email)?.takeIf { it.isNotBlank() },
                    countryIso = env.user?.countryIso ?: env.countryIso ?: "BD",
                    fideFederation = env.user?.fideFederation ?: env.fideFederation ?: "BAN",
                    isAdmin = env.user?.isAdmin ?: env.isAdmin ?: false,
                ),
                serverUrl = normalizedUrl,
                expiresAtEpochMs = env.expiresAtEpochMs ?: 0L,
            )
            persistSession(session)
            _authState.value = AuthState.Authenticated(session)
            session
        }
    }

    override suspend fun signOut() {
        withContext(ioDispatcher) {
            val current = (_authState.value as? AuthState.Authenticated)?.session
            if (current != null) {
                runCatching {
                    val req = Request.Builder()
                        .url("${current.serverUrl}/v1/auth/logout")
                        .header("Authorization", "Bearer ${current.accessToken}")
                        .post("{}".toRequestBody(JSON_MEDIA))
                        .build()
                    okHttpClient.newCall(req).execute().close()
                }
            }
            clearStoredSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun executeAuthPost(
        endpoint: String,
        payloadJson: String,
        serverUrl: String,
    ): AuthSession {
        val req = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .post(payloadJson.toRequestBody(JSON_MEDIA))
            .build()

        val raw = try {
            okHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException(parseServerError(body, resp.code, serverUrl))
                }
                body
            }
        } catch (io: IOException) {
            throw IllegalStateException(
                "Unable to reach ChessHub server at $serverUrl (${io.localizedMessage ?: "connection failed"}). Check your server URL or Continue Offline.",
                io,
            )
        }

        return parseSessionResponse(raw, serverUrl)
    }

    private suspend fun persistSession(session: AuthSession) {
        // Security: Only bearer token and public user claims are saved; passwords are never stored.
        appContext.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            if (session.refreshToken != null) prefs[Keys.REFRESH_TOKEN] = session.refreshToken else prefs.remove(Keys.REFRESH_TOKEN)
            prefs[Keys.USER_ID] = session.user.uid
            prefs[Keys.USERNAME] = session.user.username
            if (session.user.email != null) prefs[Keys.EMAIL] = session.user.email else prefs.remove(Keys.EMAIL)
            prefs[Keys.COUNTRY_ISO] = session.user.countryIso
            prefs[Keys.FIDE_FED] = session.user.fideFederation
            prefs[Keys.IS_ADMIN] = session.user.isAdmin
            prefs[Keys.SERVER_URL] = session.serverUrl
            prefs[Keys.EXPIRES_AT] = session.expiresAtEpochMs
        }
    }

    private suspend fun clearStoredSession() {
        appContext.authDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.IS_ADMIN)
            prefs.remove(Keys.EXPIRES_AT)
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val authMoshi = ChessHubApiClientFactory.createMoshi()

        fun defaultAuthOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        internal fun parseSessionResponse(rawJson: String, serverUrl: String): AuthSession {
            val env = authMoshi.adapter(AuthSessionEnvelope::class.java).fromJson(rawJson)
                ?: error("Empty session response from server.")
            val accessToken = env.accessToken?.takeIf { it.isNotBlank() }
                ?: env.token?.takeIf { it.isNotBlank() }
                ?: error("Missing access token in server response.")
            val uid = env.user?.userId?.takeIf { it.isNotBlank() }
                ?: env.user?.uid?.takeIf { it.isNotBlank() }
                ?: env.userId?.takeIf { it.isNotBlank() }
                ?: env.uid?.takeIf { it.isNotBlank() }
                ?: error("Missing user id in server response.")
            val user = AuthUser(
                uid = uid,
                username = env.user?.username ?: env.username ?: "Player",
                email = (env.user?.email ?: env.email)?.takeIf { it.isNotBlank() },
                countryIso = env.user?.countryIso ?: env.countryIso ?: "BD",
                fideFederation = env.user?.fideFederation ?: env.fideFederation ?: "BAN",
                isAdmin = env.user?.isAdmin ?: env.isAdmin ?: false,
            )
            return AuthSession(
                accessToken = accessToken,
                refreshToken = env.refreshToken?.takeIf { it.isNotBlank() },
                user = user,
                serverUrl = serverUrl,
                expiresAtEpochMs = env.expiresAtEpochMs ?: 0L,
            )
        }

        internal fun parseServerError(rawBody: String, statusCode: Int, serverUrl: String): String {
            val msg = runCatching {
                val err = authMoshi.adapter(AuthErrorEnvelope::class.java).fromJson(rawBody)
                err?.message?.takeIf { it.isNotBlank() } ?: err?.error.orEmpty()
            }.getOrNull().orEmpty()
            return if (msg.isNotBlank()) msg else "Server at $serverUrl returned HTTP $statusCode"
        }
    }
}
