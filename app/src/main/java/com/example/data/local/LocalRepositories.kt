package com.example.data.local

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.Glicko2RatingSystem
import com.example.chess.GlickoRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chesshub_prefs")

data class StudyChapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val initialFen: String,
    val pgn: String,
    val notes: String = "",
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val highContrast: Boolean = false,
    val dynamicColor: Boolean = false,
    val boardThemeId: String = "emerald_slate",
    val pieceStyleId: String = "neo",
    val showCoordinates: Boolean = true,
    val showLegalMoves: Boolean = true,
    val showLastMove: Boolean = true,
    val moveAnimation: Boolean = true,
    val lowMemoryMode: Boolean = false,
    val autoQueen: Boolean = false,
    val confirmMoves: Boolean = false,
    val premoveEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val hapticsEnabled: Boolean = true,
    val showEvalBar: Boolean = true,
    val showThreatArrow: Boolean = false,
    val coordinateNotationOnly: Boolean = false,
    val fontScale: Float = 1.0f,
    val boardScale: Float = 1.0f,
    val languageCode: String = "en",
    val enableStreakPenalty: Boolean = false,
    val showSocialFeed: Boolean = true,
    val allowChatInGames: Boolean = true,
    val countryIso: String = "BD",
    val fideFederation: String = "BAN",
    val hiddenHomeCards: Set<String> = emptySet(),
    val homeCardOrder: List<String> = listOf(
        "active_game", "daily_puzzle", "recent_games",
        "recommended_lesson", "upcoming_tournament", "saved_analysis", "friend_activity",
    ),
    val localPuzzleRating: Int = 1200,
    val localPuzzleRd: Double = 250.0,
    val puzzleBestStreak: Int = 0,
    val puzzleBestStorm: Int = 0,
    val favoriteTimeControls: List<String> = listOf("1+0", "3+2", "5+0", "10+0"),
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SHOW_COORDS = booleanPreferencesKey("show_coords")
        val SHOW_LEGAL = booleanPreferencesKey("show_legal")
        val SHOW_LAST_MOVE = booleanPreferencesKey("show_last_move")
        val MOVE_ANIMATION = booleanPreferencesKey("move_animation")
        val LOW_MEMORY = booleanPreferencesKey("low_memory")
        val AUTO_QUEEN = booleanPreferencesKey("auto_queen")
        val CONFIRM_MOVES = booleanPreferencesKey("confirm_moves")
        val PREMOVE = booleanPreferencesKey("premove")
        val SOUND = booleanPreferencesKey("sound")
        val SOUND_VOL = floatPreferencesKey("sound_vol")
        val HAPTICS = booleanPreferencesKey("haptics")
        val EVAL_BAR = booleanPreferencesKey("eval_bar")
        val THREAT_ARROW = booleanPreferencesKey("threat_arrow")
        val COORD_NOTATION = booleanPreferencesKey("coord_notation")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val BOARD_SCALE = floatPreferencesKey("board_scale")
        val LANGUAGE = stringPreferencesKey("language")
        val STREAK_PENALTY = booleanPreferencesKey("streak_penalty")
        val SOCIAL_FEED = booleanPreferencesKey("social_feed")
        val ALLOW_CHAT = booleanPreferencesKey("allow_chat")
        val COUNTRY_ISO = stringPreferencesKey("country_iso")
        val FIDE_FED = stringPreferencesKey("fide_fed")
        val HIDDEN_CARDS = stringPreferencesKey("hidden_cards")
        val CARD_ORDER = stringPreferencesKey("card_order")
        val PUZZLE_RATING = intPreferencesKey("puzzle_rating")
        val PUZZLE_RD = floatPreferencesKey("puzzle_rd")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val BEST_STORM = intPreferencesKey("best_storm")
    }

    val isDeviceLowRam: Boolean by lazy {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        (am?.isLowRamDevice == true) || (memInfo.totalMem > 0 && memInfo.totalMem <= 1_400_000_000L)
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaultLowMem = isDeviceLowRam
        val lowMem = prefs[Keys.LOW_MEMORY] ?: defaultLowMem
        AppSettings(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            boardThemeId = prefs[Keys.BOARD_THEME] ?: "emerald_slate",
            pieceStyleId = prefs[Keys.PIECE_STYLE] ?: "neo",
            showCoordinates = prefs[Keys.SHOW_COORDS] ?: true,
            showLegalMoves = prefs[Keys.SHOW_LEGAL] ?: true,
            showLastMove = prefs[Keys.SHOW_LAST_MOVE] ?: true,
            moveAnimation = prefs[Keys.MOVE_ANIMATION] ?: !lowMem,
            lowMemoryMode = lowMem,
            autoQueen = prefs[Keys.AUTO_QUEEN] ?: false,
            confirmMoves = prefs[Keys.CONFIRM_MOVES] ?: false,
            premoveEnabled = prefs[Keys.PREMOVE] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            soundVolume = prefs[Keys.SOUND_VOL] ?: 0.8f,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: !lowMem,
            showEvalBar = prefs[Keys.EVAL_BAR] ?: true,
            showThreatArrow = prefs[Keys.THREAT_ARROW] ?: false,
            coordinateNotationOnly = prefs[Keys.COORD_NOTATION] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            boardScale = prefs[Keys.BOARD_SCALE] ?: 1.0f,
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            enableStreakPenalty = prefs[Keys.STREAK_PENALTY] ?: false,
            showSocialFeed = prefs[Keys.SOCIAL_FEED] ?: true,
            allowChatInGames = prefs[Keys.ALLOW_CHAT] ?: false, // conservative default
            countryIso = prefs[Keys.COUNTRY_ISO] ?: "BD",
            fideFederation = prefs[Keys.FIDE_FED] ?: "BAN",
            hiddenHomeCards = prefs[Keys.HIDDEN_CARDS]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            homeCardOrder = prefs[Keys.CARD_ORDER]?.split(',')?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() } ?: AppSettings().homeCardOrder,
            localPuzzleRating = prefs[Keys.PUZZLE_RATING] ?: 1200,
            localPuzzleRd = (prefs[Keys.PUZZLE_RD] ?: 250f).toDouble(),
            puzzleBestStreak = prefs[Keys.BEST_STREAK] ?: 0,
            puzzleBestStorm = prefs[Keys.BEST_STORM] ?: 0,
        )
    }

    suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setDarkTheme(v: Boolean) = update { it[Keys.DARK_THEME] = v }
    suspend fun setHighContrast(v: Boolean) = update { it[Keys.HIGH_CONTRAST] = v }
    suspend fun setBoardTheme(id: String) = update { it[Keys.BOARD_THEME] = id }
    suspend fun setPieceStyle(id: String) = update { it[Keys.PIECE_STYLE] = id }
    suspend fun setShowCoordinates(v: Boolean) = update { it[Keys.SHOW_COORDS] = v }
    suspend fun setShowLegalMoves(v: Boolean) = update { it[Keys.SHOW_LEGAL] = v }
    suspend fun setMoveAnimation(v: Boolean) = update { it[Keys.MOVE_ANIMATION] = v }
    suspend fun setLowMemoryMode(v: Boolean) = update { it[Keys.LOW_MEMORY] = v }
    suspend fun setAutoQueen(v: Boolean) = update { it[Keys.AUTO_QUEEN] = v }
    suspend fun setConfirmMoves(v: Boolean) = update { it[Keys.CONFIRM_MOVES] = v }
    suspend fun setPremoveEnabled(v: Boolean) = update { it[Keys.PREMOVE] = v }
    suspend fun setSoundEnabled(v: Boolean) = update { it[Keys.SOUND] = v }
    suspend fun setSoundVolume(v: Float) = update { it[Keys.SOUND_VOL] = v }
    suspend fun setHapticsEnabled(v: Boolean) = update { it[Keys.HAPTICS] = v }
    suspend fun setShowEvalBar(v: Boolean) = update { it[Keys.EVAL_BAR] = v }
    suspend fun setShowThreatArrow(v: Boolean) = update { it[Keys.THREAT_ARROW] = v }
    suspend fun setCoordinateNotation(v: Boolean) = update { it[Keys.COORD_NOTATION] = v }
    suspend fun setFontScale(v: Float) = update { it[Keys.FONT_SCALE] = v }
    suspend fun setLanguage(code: String) = update { it[Keys.LANGUAGE] = code }
    suspend fun setShowSocialFeed(v: Boolean) = update { it[Keys.SOCIAL_FEED] = v }
    suspend fun setAllowChatInGames(v: Boolean) = update { it[Keys.ALLOW_CHAT] = v }
    suspend fun setCountryAndFide(countryIso: String, fideFed: String) = update {
        it[Keys.COUNTRY_ISO] = countryIso
        it[Keys.FIDE_FED] = fideFed
    }

    suspend fun toggleHomeCard(cardId: String, currentHidden: Set<String>) = update {
        val next = if (cardId in currentHidden) currentHidden - cardId else currentHidden + cardId
        it[Keys.HIDDEN_CARDS] = next.joinToString(",")
    }

    suspend fun setHomeCardOrder(order: List<String>) = update {
        it[Keys.CARD_ORDER] = order.joinToString(",")
    }

    suspend fun recordPuzzleResult(puzzleRating: Int, solved: Boolean, streakNow: Int = 0, stormNow: Int = 0): Int {
        var newRatingInt = 1200
        update { prefs ->
            val currR = (prefs[Keys.PUZZLE_RATING] ?: 1200).toDouble()
            val currRd = (prefs[Keys.PUZZLE_RD] ?: 250f).toDouble()
            val updated = Glicko2RatingSystem.updateSingleGame(
                player = GlickoRating(currR, currRd, 0.06),
                opponent = GlickoRating(puzzleRating.toDouble(), 80.0, 0.06),
                score = if (solved) 1.0 else 0.0,
            )
            newRatingInt = updated.displayInt
            prefs[Keys.PUZZLE_RATING] = newRatingInt
            prefs[Keys.PUZZLE_RD] = updated.rd.toFloat()
            val bestStreak = prefs[Keys.BEST_STREAK] ?: 0
            if (streakNow > bestStreak) prefs[Keys.BEST_STREAK] = streakNow
            val bestStorm = prefs[Keys.BEST_STORM] ?: 0
            if (stormNow > bestStorm) prefs[Keys.BEST_STORM] = stormNow
        }
        return newRatingInt
    }
}

class LocalChessRepository(private val dao: ChessHubDao) {
    fun observeGames(limit: Int = 100): Flow<List<LocalGameEntity>> = dao.observeRecentGames(limit)
    fun observeStudies(): Flow<List<LocalStudyEntity>> = dao.observeStudies()
    fun observeCollections(): Flow<List<CollectionEntity>> = dao.observeCollections()
    fun observePuzzleAttempts(): Flow<List<PuzzleAttemptEntity>> = dao.observePuzzleAttempts()
    fun observeCustomPuzzles(): Flow<List<CustomPuzzleEntity>> = dao.observeCustomPuzzles()
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>> = dao.observeLessonProgress()

    suspend fun saveGame(game: LocalGameEntity) = dao.upsertGame(game)
    suspend fun getGame(id: String): LocalGameEntity? = dao.getGameById(id)
    suspend fun deleteGame(id: String) = dao.deleteGame(id)
    suspend fun saveStudy(study: LocalStudyEntity) = dao.upsertStudy(study)
    suspend fun deleteStudy(id: String) = dao.deleteStudy(id)
    suspend fun saveCollection(col: CollectionEntity) = dao.upsertCollection(col)
    suspend fun deleteCollection(id: String) = dao.deleteCollection(id)
    suspend fun recordPuzzleAttempt(attempt: PuzzleAttemptEntity) = dao.insertPuzzleAttempt(attempt)
    suspend fun saveCustomPuzzle(puzzle: CustomPuzzleEntity) = dao.upsertCustomPuzzle(puzzle)
    suspend fun saveLessonProgress(progress: LessonProgressEntity) = dao.upsertLessonProgress(progress)

    companion object {
        fun encodeChapters(chapters: List<StudyChapter>): String {
            val arr = JSONArray()
            chapters.forEach { ch ->
                arr.put(
                    JSONObject().apply {
                        put("id", ch.id)
                        put("title", ch.title)
                        put("initialFen", ch.initialFen)
                        put("pgn", ch.pgn)
                        put("notes", ch.notes)
                    },
                )
            }
            return arr.toString()
        }

        fun decodeChapters(json: String): List<StudyChapter> = runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                StudyChapter(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Chapter ${i + 1}"),
                    initialFen = obj.optString("initialFen", com.example.chess.ChessBoardState.STANDARD_FEN),
                    pgn = obj.optString("pgn", ""),
                    notes = obj.optString("notes", ""),
                )
            }
        }.getOrDefault(emptyList())
    }
}
