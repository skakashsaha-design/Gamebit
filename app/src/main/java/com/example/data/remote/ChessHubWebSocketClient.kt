package com.example.data.remote

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

@JsonClass(generateAdapter = true)
internal data class WsClientEnvelope(
    val type: String,
    val gameId: String? = null,
    val ticketId: String? = null,
    val moveUci: String? = null,
    val clientTimestampMs: Long? = null,
    val timestampMs: Long? = null,
)

class ChessHubWebSocketClient(
    private val serverUrl: String,
    private val accessTokenProvider: () -> String,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi = ChessHubApiClientFactory.createMoshi(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ChessHubWsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChessHubWsEvent> = _events.asSharedFlow()

    private val shouldStayConnected = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var activeGameSubscription: String? = null
    private var activeTicketSubscription: String? = null
    private var reconnectAttempt = 0
    private val clientEnvelopeAdapter = moshi.adapter(WsClientEnvelope::class.java)

    fun connect() {
        shouldStayConnected.set(true)
        if (_connectionState.value == WebSocketConnectionState.CONNECTED ||
            _connectionState.value == WebSocketConnectionState.CONNECTING
        ) {
            return
        }
        openSocket()
    }

    fun disconnect() {
        shouldStayConnected.set(false)
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client closed")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    fun subscribeGame(gameId: String) {
        activeGameSubscription = gameId
        sendEnvelope(WsClientEnvelope(type = "subscribe_game", gameId = gameId))
    }

    fun unsubscribeGame(gameId: String) {
        if (activeGameSubscription == gameId) activeGameSubscription = null
        sendEnvelope(WsClientEnvelope(type = "unsubscribe_game", gameId = gameId))
    }

    fun subscribeMatchmaking(ticketId: String) {
        activeTicketSubscription = ticketId
        sendEnvelope(WsClientEnvelope(type = "subscribe_ticket", ticketId = ticketId))
    }

    fun sendMove(gameId: String, moveUci: String) {
        sendEnvelope(
            WsClientEnvelope(
                type = "submit_move",
                gameId = gameId,
                moveUci = moveUci,
                clientTimestampMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun openSocket() {
        val wsUrl = toWebSocketUrl(serverUrl)
        val token = accessTokenProvider()
        _connectionState.value = if (reconnectAttempt == 0) {
            WebSocketConnectionState.CONNECTING
        } else {
            WebSocketConnectionState.RECONNECTING
        }

        val req = Request.Builder()
            .url(wsUrl)
            .apply {
                if (token.isNotBlank()) header("Authorization", "Bearer $token")
            }
            .build()

        webSocket = okHttpClient.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    _connectionState.value = WebSocketConnectionState.CONNECTED
                    scope.launch { _events.emit(ChessHubWsEvent.Connected) }
                    activeGameSubscription?.let { subscribeGame(it) }
                    activeTicketSubscription?.let { subscribeMatchmaking(it) }
                    startHeartbeat()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val parsed = parseServerEvent(text, moshi) ?: return
                    scope.launch { _events.emit(parsed) }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    heartbeatJob?.cancel()
                    scheduleReconnectIfNeeded()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket disconnected: ${t.localizedMessage}")
                    heartbeatJob?.cancel()
                    scheduleReconnectIfNeeded()
                }
            },
        )
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (shouldStayConnected.get() && _connectionState.value == WebSocketConnectionState.CONNECTED) {
                delay(20_000L)
                sendEnvelope(WsClientEnvelope(type = "heartbeat", timestampMs = System.currentTimeMillis()))
            }
        }
    }

    private fun scheduleReconnectIfNeeded() {
        if (!shouldStayConnected.get()) {
            _connectionState.value = WebSocketConnectionState.DISCONNECTED
            return
        }
        _connectionState.value = WebSocketConnectionState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (2_000L shl reconnectAttempt.coerceAtMost(4)).coerceAtMost(30_000L)
            reconnectAttempt++
            delay(backoffMs)
            if (shouldStayConnected.get()) openSocket()
        }
    }

    private fun sendEnvelope(envelope: WsClientEnvelope) {
        webSocket?.send(clientEnvelopeAdapter.toJson(envelope))
    }

    companion object {
        private const val TAG = "ChessHubWS"
        private val MAP_TYPE = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

        fun toWebSocketUrl(baseUrl: String): String {
            val clean = baseUrl.trim().trimEnd('/')
            return when {
                clean.startsWith("https://", ignoreCase = true) ->
                    "wss://${clean.removePrefix("https://")}/v1/ws"
                clean.startsWith("http://", ignoreCase = true) ->
                    "ws://${clean.removePrefix("http://")}/v1/ws"
                else -> "wss://$clean/v1/ws"
            }
        }

        fun parseServerEvent(
            rawJson: String,
            moshi: Moshi = ChessHubApiClientFactory.createMoshi(),
        ): ChessHubWsEvent? = runCatching {
            val mapAdapter = moshi.adapter<Map<String, Any?>>(MAP_TYPE)
            val root = mapAdapter.fromJson(rawJson) ?: return null
            val type = root["type"] as? String ?: return null
            val gameAdapter = moshi.adapter(OnlineGameDoc::class.java)
            val deltaAdapter = moshi.adapter(RatingDeltaResult::class.java)
            val chatAdapter = moshi.adapter(GameChatMessageDoc::class.java)
            val dmAdapter = moshi.adapter(DirectMessageDoc::class.java)

            when (type) {
                "connected" -> ChessHubWsEvent.Connected
                "match_found" -> {
                    val game = root["game"]?.let { gameAdapter.fromJsonValue(it) } ?: return null
                    ChessHubWsEvent.MatchFound(
                        ticketId = (root["ticketId"] as? String).orEmpty(),
                        game = game,
                    )
                }
                "game_snapshot" -> {
                    val game = root["game"]?.let { gameAdapter.fromJsonValue(it) } ?: return null
                    ChessHubWsEvent.GameSnapshot(game)
                }
                "move_broadcast" -> {
                    val game = root["game"]?.let { gameAdapter.fromJsonValue(it) } ?: return null
                    val delta = root["ratingDelta"]?.let { deltaAdapter.fromJsonValue(it) }
                    ChessHubWsEvent.MoveBroadcast(game, delta)
                }
                "clock_sync" -> {
                    val gameId = root["gameId"] as? String ?: return null
                    val whiteMs = (root["whiteTimeMs"] as? Number)?.toLong() ?: return null
                    val blackMs = (root["blackTimeMs"] as? Number)?.toLong() ?: return null
                    ChessHubWsEvent.ClockSync(
                        gameId = gameId,
                        whiteTimeMs = whiteMs,
                        blackTimeMs = blackMs,
                        turn = (root["turn"] as? String).orEmpty().ifBlank { "w" },
                        serverTimestampMs = (root["serverTimestampMs"] as? Number)?.toLong()
                            ?: System.currentTimeMillis(),
                    )
                }
                "game_ended" -> {
                    val game = root["game"]?.let { gameAdapter.fromJsonValue(it) } ?: return null
                    val delta = root["ratingDelta"]?.let { deltaAdapter.fromJsonValue(it) }
                    ChessHubWsEvent.GameEnded(game, delta)
                }
                "game_chat" -> {
                    val msg = root["message"]?.let { chatAdapter.fromJsonValue(it) } ?: return null
                    ChessHubWsEvent.GameChatReceived(
                        gameId = (root["gameId"] as? String).orEmpty(),
                        message = msg,
                    )
                }
                "direct_message" -> {
                    val msg = root["message"]?.let { dmAdapter.fromJsonValue(it) } ?: return null
                    ChessHubWsEvent.DirectMessageReceived(
                        threadId = (root["threadId"] as? String).orEmpty(),
                        message = msg,
                    )
                }
                "presence" -> ChessHubWsEvent.PresenceChanged(
                    userId = root["userId"] as? String ?: return null,
                    onlineStatus = (root["onlineStatus"] as? String).orEmpty().ifBlank { "online" },
                )
                "error" -> ChessHubWsEvent.ServerError(
                    code = (root["code"] as? String).orEmpty().ifBlank { "SERVER_ERROR" },
                    message = (root["message"] as? String).orEmpty().ifBlank { "Unknown server error" },
                )
                else -> null
            }
        }.getOrNull()
    }
}
