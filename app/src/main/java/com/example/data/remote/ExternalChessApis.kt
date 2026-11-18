package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExternalExplorerMove(
    val uci: String,
    val san: String,
    val whiteWins: Int,
    val draws: Int,
    val blackWins: Int,
    val averageRating: Int,
) {
    val totalGames: Int get() = (whiteWins + draws + blackWins).coerceAtLeast(1)
}

data class ExternalExplorerResult(
    val openingName: String,
    val eco: String,
    val moves: List<ExternalExplorerMove>,
    val topGames: List<String>,
)

data class ExternalTablebaseResult(
    val category: String,
    val dtm: Int?,
    val dtz: Int?,
    val bestMoveUci: String?,
    val bestMoveSan: String?,
)

data class ExternalBroadcastItem(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val tier: Int,
)

data class ExternalTvChannel(
    val channelName: String,
    val playerWhite: String,
    val playerBlack: String,
    val gameId: String,
)

data class ExternalPlayerSummary(
    val platform: String,
    val username: String,
    val ratingSummary: String,
    val gamesCount: Int,
    val url: String,
    val recentPgns: List<String>,
)

/**
 * Uses ONLY official, unauthenticated public endpoints of Lichess and Chess.com.
 * Respects rate limits and clearly labels external data. Never scrapes private endpoints.
 */
class ExternalChessApis {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private suspend fun getJson(url: String, accept: String = "application/json"): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("Accept", accept)
                .header("User-Agent", "ChessHub-Android/2.0 (Independent Open Chess Client)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}: ${resp.message}")
                resp.body?.string() ?: error("Empty response body")
            }
        }
    }

    suspend fun fetchLichessDailyPuzzle(): Result<com.example.data.local.TacticalPuzzle> {
        return getJson("https://lichess.org/api/puzzle/daily").mapCatching { body ->
            val root = JSONObject(body)
            val gamePgn = root.getJSONObject("game").getString("pgn")
            val puzzle = root.getJSONObject("puzzle")
            val initialPly = puzzle.getInt("initialPly")
            val solutionArr = puzzle.getJSONArray("solution")
            val themesArr = puzzle.getJSONArray("themes")
            val solList = (0 until solutionArr.length()).map { solutionArr.getString(it) }
            val themes = (0 until themesArr.length()).map { themesArr.getString(it) }

            // Replay PGN up to initialPly to derive exact puzzle FEN
            var state = com.example.chess.ChessBoardState.standard()
            val tokens = gamePgn.split(Regex("\\s+")).filter { it.isNotBlank() }
            var plies = 0
            for (tok in tokens) {
                if (plies >= initialPly) break
                val move = com.example.chess.MoveGenerator.parseSan(state, tok) ?: break
                state = com.example.chess.MoveGenerator.applyMoveUnchecked(state, move)
                plies++
            }
            com.example.data.local.TacticalPuzzle(
                id = "lichess_daily_${puzzle.getString("id")}",
                title = "Lichess Daily Puzzle",
                fen = state.toFen(),
                solutionUci = solList,
                rating = puzzle.getInt("rating"),
                themes = themes.ifEmpty { listOf("Tactics") },
                hint = "Find the sharpest continuation from Lichess Daily.",
                explanation = "Official Lichess Daily Puzzle (#${puzzle.getString("id")}).",
            )
        }
    }

    suspend fun queryOpeningExplorer(fen: String, mastersOnly: Boolean): Result<ExternalExplorerResult> {
        val encoded = java.net.URLEncoder.encode(fen, "UTF-8")
        val endpoint = if (mastersOnly) "masters" else "lichess"
        return getJson("https://explorer.lichess.ovh/$endpoint?fen=$encoded&topGames=5").mapCatching { body ->
            val obj = JSONObject(body)
            val opening = obj.optJSONObject("opening")
            val movesArr = obj.optJSONArray("moves") ?: JSONArray()
            val moves = (0 until movesArr.length()).map { i ->
                val m = movesArr.getJSONObject(i)
                ExternalExplorerMove(
                    uci = m.getString("uci"),
                    san = m.getString("san"),
                    whiteWins = m.optInt("white", 0),
                    draws = m.optInt("draws", 0),
                    blackWins = m.optInt("black", 0),
                    averageRating = m.optInt("averageRating", 0),
                )
            }
            val topArr = obj.optJSONArray("topGames") ?: JSONArray()
            val topGames = (0 until topArr.length()).map { i ->
                val g = topArr.getJSONObject(i)
                val w = g.optJSONObject("white")?.optString("name", "?") ?: "?"
                val b = g.optJSONObject("black")?.optString("name", "?") ?: "?"
                val yr = g.optInt("year", 0)
                "$w vs $b ($yr)"
            }
            ExternalExplorerResult(
                openingName = opening?.optString("name", "Unknown") ?: "Custom / Out of Book",
                eco = opening?.optString("eco", "-") ?: "-",
                moves = moves,
                topGames = topGames,
            )
        }
    }

    suspend fun queryTablebase(fen: String): Result<ExternalTablebaseResult> {
        val encoded = java.net.URLEncoder.encode(fen, "UTF-8")
        return getJson("https://tablebase.lichess.ovh/standard?fen=$encoded").mapCatching { body ->
            val obj = JSONObject(body)
            val category = obj.optString("category", "unknown")
            val dtm = if (obj.has("dtm") && !obj.isNull("dtm")) obj.optInt("dtm") else null
            val dtz = if (obj.has("dtz") && !obj.isNull("dtz")) obj.optInt("dtz") else null
            val moves = obj.optJSONArray("moves")
            val best = if (moves != null && moves.length() > 0) moves.getJSONObject(0) else null
            ExternalTablebaseResult(
                category = category,
                dtm = dtm,
                dtz = dtz,
                bestMoveUci = best?.optString("uci"),
                bestMoveSan = best?.optString("san"),
            )
        }
    }

    suspend fun fetchLichessBroadcasts(): Result<List<ExternalBroadcastItem>> {
        return getJson("https://lichess.org/api/broadcast?nb=12").mapCatching { body ->
            val items = mutableListOf<ExternalBroadcastItem>()
            body.lines().filter { it.isNotBlank() }.forEach { line ->
                val obj = JSONObject(line)
                val tour = obj.optJSONObject("tour") ?: return@forEach
                items.add(
                    ExternalBroadcastItem(
                        id = tour.optString("id", ""),
                        name = tour.optString("name", "Broadcast"),
                        description = tour.optString("description", ""),
                        url = tour.optString("url", "https://lichess.org/broadcast"),
                        tier = tour.optInt("tier", 3),
                    ),
                )
            }
            items
        }
    }

    suspend fun fetchLichessTvChannels(): Result<List<ExternalTvChannel>> {
        return getJson("https://lichess.org/api/tv/channels").mapCatching { body ->
            val obj = JSONObject(body)
            val list = mutableListOf<ExternalTvChannel>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val ch = obj.optJSONObject(key) ?: continue
                val user = ch.optJSONObject("user")?.optString("name", "Player") ?: "Player"
                val rating = ch.optInt("rating", 0)
                list.add(
                    ExternalTvChannel(
                        channelName = key.replaceFirstChar { it.uppercase() },
                        playerWhite = "$user ($rating)",
                        playerBlack = "Featured Opponent",
                        gameId = ch.optString("gameId", ""),
                    ),
                )
            }
            list
        }
    }

    suspend fun fetchLichessProfileAndGames(username: String): Result<ExternalPlayerSummary> {
        val clean = username.trim()
        require(clean.isNotEmpty()) { "Enter a Lichess username." }
        return getJson("https://lichess.org/api/user/$clean").mapCatching { userBody ->
            val obj = JSONObject(userBody)
            val perfs = obj.optJSONObject("perfs")
            val blitz = perfs?.optJSONObject("blitz")?.optInt("rating")
            val rapid = perfs?.optJSONObject("rapid")?.optInt("rating")
            val bullet = perfs?.optJSONObject("bullet")?.optInt("rating")
            val count = obj.optJSONObject("count")?.optInt("all") ?: 0
            val ratingSummary = buildString {
                bullet?.let { append("Bullet $it  ") }
                blitz?.let { append("Blitz $it  ") }
                rapid?.let { append("Rapid $it") }
            }.ifBlank { "Unrated" }

            val pgnBody = getJson("https://lichess.org/api/games/user/$clean?max=5&opening=true", accept = "application/x-chess-pgn")
                .getOrDefault("")
            val pgns = pgnBody.split(Regex("\n\n(?=\n*\\[Event )")).filter { it.isNotBlank() }

            ExternalPlayerSummary(
                platform = "Lichess",
                username = obj.optString("username", clean),
                ratingSummary = ratingSummary,
                gamesCount = count,
                url = obj.optString("url", "https://lichess.org/@/$clean"),
                recentPgns = pgns,
            )
        }
    }

    suspend fun fetchChessComProfileAndGames(username: String): Result<ExternalPlayerSummary> {
        val clean = username.trim().lowercase()
        require(clean.isNotEmpty()) { "Enter a Chess.com username." }
        return getJson("https://api.chess.com/pub/player/$clean").mapCatching { profileBody ->
            val pObj = JSONObject(profileBody)
            val statsBody = getJson("https://api.chess.com/pub/player/$clean/stats").getOrDefault("{}")
            val sObj = JSONObject(statsBody)
            val bullet = sObj.optJSONObject("chess_bullet")?.optJSONObject("last")?.optInt("rating")
            val blitz = sObj.optJSONObject("chess_blitz")?.optJSONObject("last")?.optInt("rating")
            val rapid = sObj.optJSONObject("chess_rapid")?.optJSONObject("last")?.optInt("rating")
            val summary = buildString {
                bullet?.let { append("Bullet $it  ") }
                blitz?.let { append("Blitz $it  ") }
                rapid?.let { append("Rapid $it") }
            }.ifBlank { "No public ratings" }

            val recentPgns = mutableListOf<String>()
            val archivesStr = getJson("https://api.chess.com/pub/player/$clean/games/archives").getOrNull()
            if (archivesStr != null) {
                val arr = JSONObject(archivesStr).optJSONArray("archives")
                if (arr != null && arr.length() > 0) {
                    val latestUrl = arr.getString(arr.length() - 1)
                    val monthJson = getJson(latestUrl).getOrNull()
                    if (monthJson != null) {
                        val games = JSONObject(monthJson).optJSONArray("games")
                        if (games != null) {
                            val start = (games.length() - 5).coerceAtLeast(0)
                            for (i in (games.length() - 1) downTo start) {
                                val pgn = games.getJSONObject(i).optString("pgn", "")
                                if (pgn.isNotBlank()) recentPgns.add(pgn)
                            }
                        }
                    }
                }
            }

            ExternalPlayerSummary(
                platform = "Chess.com",
                username = pObj.optString("username", clean),
                ratingSummary = summary,
                gamesCount = recentPgns.size,
                url = pObj.optString("url", "https://www.chess.com/member/$clean"),
                recentPgns = recentPgns,
            )
        }
    }
}
