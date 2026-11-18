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
import kotlinx.coroutines.flow.flowOf
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
    val pieceStyleId: String = "classic", // classic, modern, minimal, wooden, high_contrast, 3d
    val showCoordinates: Boolean = true,
    val showLegalMoves: Boolean = true,
    val showLastMove: Boolean = true,
    val moveAnimation: Boolean = true,
    val reducedAnimations: Boolean = false,
    val animationSpeed: Float = 1.0f,
    val lowMemoryMode: Boolean = false,
    val autoQueen: Boolean = false,
    val confirmMoves: Boolean = false,
    val premoveEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val sfxMove: Boolean = true,
    val sfxCapture: Boolean = true,
    val sfxCheck: Boolean = true,
    val sfxCheckmate: Boolean = true,
    val sfxPromotion: Boolean = true,
    val sfxFeedback: Boolean = true,
    val sfxButton: Boolean = true,
    val sfxBookMove: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val botVoiceEnabled: Boolean = true,
    val trainerVoiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.85f,
    val musicEnabled: Boolean = false,
    val musicVolume: Float = 0.55f,
    val musicInMenu: Boolean = true,
    val musicInGame: Boolean = false,
    val musicInTraining: Boolean = true,
    val lowMemoryAudioMode: Boolean = false,
    val showEvalBar: Boolean = true,
    val showThreatArrow: Boolean = false,
    val showBestMoveArrow: Boolean = true,
    val allowBoardArrows: Boolean = true,
    val engineDepth: Int = 3,
    val engineMultiPv: Int = 3,
    val engineThreads: Int = 1,
    val engineHashMb: Int = 16,
    val openingBookEnabled: Boolean = true,
    val openingRecognitionEnabled: Boolean = true,
    val gambitRecognitionEnabled: Boolean = true,
    val endgameRecognitionEnabled: Boolean = true,
    val coordinateNotationOnly: Boolean = false,
    val fontScale: Float = 1.0f,
    val boardScale: Float = 1.0f,
    val languageCode: String = "en",
    val enableStreakPenalty: Boolean = false,
    val showSocialFeed: Boolean = true,
    val allowChatInGames: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val countryIso: String = "BD",
    val fideFederation: String = "BAN",
    val localAvatarId: String = "king",
    val localDisplayName: String = "ChessHub Player",
    val localBio: String = "",
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
        val REDUCED_ANIMATIONS = booleanPreferencesKey("reduced_animations")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val LOW_MEMORY = booleanPreferencesKey("low_memory")
        val AUTO_QUEEN = booleanPreferencesKey("auto_queen")
        val CONFIRM_MOVES = booleanPreferencesKey("confirm_moves")
        val PREMOVE = booleanPreferencesKey("premove")
        val SOUND = booleanPreferencesKey("sound")
        val SOUND_VOL = floatPreferencesKey("sound_vol")
        val SFX_MOVE = booleanPreferencesKey("sfx_move")
        val SFX_CAPTURE = booleanPreferencesKey("sfx_capture")
        val SFX_CHECK = booleanPreferencesKey("sfx_check")
        val SFX_MATE = booleanPreferencesKey("sfx_mate")
        val SFX_PROMO = booleanPreferencesKey("sfx_promo")
        val SFX_FEEDBACK = booleanPreferencesKey("sfx_feedback")
        val SFX_BUTTON = booleanPreferencesKey("sfx_button")
        val SFX_BOOK = booleanPreferencesKey("sfx_book")
        val HAPTICS = booleanPreferencesKey("haptics")
        val BOT_VOICE = booleanPreferencesKey("bot_voice")
        val TRAINER_VOICE = booleanPreferencesKey("trainer_voice")
        val VOICE_VOL = floatPreferencesKey("voice_vol")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val MUSIC_VOL = floatPreferencesKey("music_vol")
        val MUSIC_MENU = booleanPreferencesKey("music_menu")
        val MUSIC_GAME = booleanPreferencesKey("music_game")
        val MUSIC_TRAIN = booleanPreferencesKey("music_train")
        val LOW_MEM_AUDIO = booleanPreferencesKey("low_mem_audio")
        val EVAL_BAR = booleanPreferencesKey("eval_bar")
        val THREAT_ARROW = booleanPreferencesKey("threat_arrow")
        val BEST_MOVE_ARROW = booleanPreferencesKey("best_move_arrow")
        val BOARD_ARROWS = booleanPreferencesKey("board_arrows")
        val ENGINE_DEPTH = intPreferencesKey("engine_depth")
        val ENGINE_MULTIPV = intPreferencesKey("engine_multipv")
        val ENGINE_THREADS = intPreferencesKey("engine_threads")
        val ENGINE_HASH_MB = intPreferencesKey("engine_hash_mb")
        val OPENING_BOOK = booleanPreferencesKey("opening_book")
        val OPENING_RECOG = booleanPreferencesKey("opening_recog")
        val GAMBIT_RECOG = booleanPreferencesKey("gambit_recog")
        val ENDGAME_RECOG = booleanPreferencesKey("endgame_recog")
        val COORD_NOTATION = booleanPreferencesKey("coord_notation")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val BOARD_SCALE = floatPreferencesKey("board_scale")
        val LANGUAGE = stringPreferencesKey("language")
        val STREAK_PENALTY = booleanPreferencesKey("streak_penalty")
        val SOCIAL_FEED = booleanPreferencesKey("social_feed")
        val ALLOW_CHAT = booleanPreferencesKey("allow_chat")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val COUNTRY_ISO = stringPreferencesKey("country_iso")
        val FIDE_FED = stringPreferencesKey("fide_fed")
        val LOCAL_AVATAR = stringPreferencesKey("local_avatar")
        val LOCAL_NAME = stringPreferencesKey("local_name")
        val LOCAL_BIO = stringPreferencesKey("local_bio")
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
            pieceStyleId = prefs[Keys.PIECE_STYLE] ?: "classic",
            showCoordinates = prefs[Keys.SHOW_COORDS] ?: true,
            showLegalMoves = prefs[Keys.SHOW_LEGAL] ?: true,
            showLastMove = prefs[Keys.SHOW_LAST_MOVE] ?: true,
            moveAnimation = prefs[Keys.MOVE_ANIMATION] ?: !lowMem,
            reducedAnimations = prefs[Keys.REDUCED_ANIMATIONS] ?: lowMem,
            animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 1.0f,
            lowMemoryMode = lowMem,
            autoQueen = prefs[Keys.AUTO_QUEEN] ?: false,
            confirmMoves = prefs[Keys.CONFIRM_MOVES] ?: false,
            premoveEnabled = prefs[Keys.PREMOVE] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            soundVolume = prefs[Keys.SOUND_VOL] ?: 0.8f,
            sfxMove = prefs[Keys.SFX_MOVE] ?: true,
            sfxCapture = prefs[Keys.SFX_CAPTURE] ?: true,
            sfxCheck = prefs[Keys.SFX_CHECK] ?: true,
            sfxCheckmate = prefs[Keys.SFX_MATE] ?: true,
            sfxPromotion = prefs[Keys.SFX_PROMO] ?: true,
            sfxFeedback = prefs[Keys.SFX_FEEDBACK] ?: true,
            sfxButton = prefs[Keys.SFX_BUTTON] ?: true,
            sfxBookMove = prefs[Keys.SFX_BOOK] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: !lowMem,
            botVoiceEnabled = prefs[Keys.BOT_VOICE] ?: !lowMem,
            trainerVoiceEnabled = prefs[Keys.TRAINER_VOICE] ?: true,
            voiceVolume = prefs[Keys.VOICE_VOL] ?: 0.85f,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
            musicVolume = prefs[Keys.MUSIC_VOL] ?: 0.55f,
            musicInMenu = prefs[Keys.MUSIC_MENU] ?: true,
            musicInGame = prefs[Keys.MUSIC_GAME] ?: false,
            musicInTraining = prefs[Keys.MUSIC_TRAIN] ?: true,
            lowMemoryAudioMode = prefs[Keys.LOW_MEM_AUDIO] ?: lowMem,
            showEvalBar = prefs[Keys.EVAL_BAR] ?: true,
            showThreatArrow = prefs[Keys.THREAT_ARROW] ?: false,
            showBestMoveArrow = prefs[Keys.BEST_MOVE_ARROW] ?: true,
            allowBoardArrows = prefs[Keys.BOARD_ARROWS] ?: true,
            engineDepth = prefs[Keys.ENGINE_DEPTH] ?: if (lowMem) 2 else 3,
            engineMultiPv = prefs[Keys.ENGINE_MULTIPV] ?: if (lowMem) 1 else 3,
            engineThreads = prefs[Keys.ENGINE_THREADS] ?: 1,
            engineHashMb = prefs[Keys.ENGINE_HASH_MB] ?: if (lowMem) 8 else 16,
            openingBookEnabled = prefs[Keys.OPENING_BOOK] ?: true,
            openingRecognitionEnabled = prefs[Keys.OPENING_RECOG] ?: true,
            gambitRecognitionEnabled = prefs[Keys.GAMBIT_RECOG] ?: true,
            endgameRecognitionEnabled = prefs[Keys.ENDGAME_RECOG] ?: true,
            coordinateNotationOnly = prefs[Keys.COORD_NOTATION] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            boardScale = prefs[Keys.BOARD_SCALE] ?: 1.0f,
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            enableStreakPenalty = prefs[Keys.STREAK_PENALTY] ?: false,
            showSocialFeed = prefs[Keys.SOCIAL_FEED] ?: true,
            allowChatInGames = prefs[Keys.ALLOW_CHAT] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            countryIso = prefs[Keys.COUNTRY_ISO] ?: "BD",
            fideFederation = prefs[Keys.FIDE_FED] ?: "BAN",
            localAvatarId = prefs[Keys.LOCAL_AVATAR] ?: "king",
            localDisplayName = prefs[Keys.LOCAL_NAME] ?: "ChessHub Player",
            localBio = prefs[Keys.LOCAL_BIO] ?: "",
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
    suspend fun setShowLastMove(v: Boolean) = update { it[Keys.SHOW_LAST_MOVE] = v }
    suspend fun setMoveAnimation(v: Boolean) = update { it[Keys.MOVE_ANIMATION] = v }
    suspend fun setReducedAnimations(v: Boolean) = update { it[Keys.REDUCED_ANIMATIONS] = v }
    suspend fun setAnimationSpeed(v: Float) = update { it[Keys.ANIMATION_SPEED] = v.coerceIn(0.5f, 2.0f) }
    suspend fun setLowMemoryMode(v: Boolean) = update { it[Keys.LOW_MEMORY] = v }
    suspend fun setAutoQueen(v: Boolean) = update { it[Keys.AUTO_QUEEN] = v }
    suspend fun setConfirmMoves(v: Boolean) = update { it[Keys.CONFIRM_MOVES] = v }
    suspend fun setPremoveEnabled(v: Boolean) = update { it[Keys.PREMOVE] = v }
    suspend fun setSoundEnabled(v: Boolean) = update { it[Keys.SOUND] = v }
    suspend fun setSoundVolume(v: Float) = update { it[Keys.SOUND_VOL] = v }
    suspend fun setSfxFlag(category: String, enabled: Boolean) = update {
        when (category) {
            "move" -> it[Keys.SFX_MOVE] = enabled
            "capture" -> it[Keys.SFX_CAPTURE] = enabled
            "check" -> it[Keys.SFX_CHECK] = enabled
            "checkmate" -> it[Keys.SFX_MATE] = enabled
            "promotion" -> it[Keys.SFX_PROMO] = enabled
            "feedback" -> it[Keys.SFX_FEEDBACK] = enabled
            "button" -> it[Keys.SFX_BUTTON] = enabled
            "book" -> it[Keys.SFX_BOOK] = enabled
        }
    }
    suspend fun setHapticsEnabled(v: Boolean) = update { it[Keys.HAPTICS] = v }
    suspend fun setBotVoiceEnabled(v: Boolean) = update { it[Keys.BOT_VOICE] = v }
    suspend fun setTrainerVoiceEnabled(v: Boolean) = update { it[Keys.TRAINER_VOICE] = v }
    suspend fun setVoiceVolume(v: Float) = update { it[Keys.VOICE_VOL] = v.coerceIn(0f, 1f) }
    suspend fun setMusicEnabled(v: Boolean) = update { it[Keys.MUSIC_ENABLED] = v }
    suspend fun setMusicVolume(v: Float) = update { it[Keys.MUSIC_VOL] = v.coerceIn(0f, 1f) }
    suspend fun setMusicScope(scopeName: String, enabled: Boolean) = update {
        when (scopeName) {
            "menu" -> it[Keys.MUSIC_MENU] = enabled
            "game" -> it[Keys.MUSIC_GAME] = enabled
            "training" -> it[Keys.MUSIC_TRAIN] = enabled
        }
    }
    suspend fun setLowMemoryAudioMode(v: Boolean) = update { it[Keys.LOW_MEM_AUDIO] = v }
    suspend fun setShowEvalBar(v: Boolean) = update { it[Keys.EVAL_BAR] = v }
    suspend fun setShowThreatArrow(v: Boolean) = update { it[Keys.THREAT_ARROW] = v }
    suspend fun setShowBestMoveArrow(v: Boolean) = update { it[Keys.BEST_MOVE_ARROW] = v }
    suspend fun setAllowBoardArrows(v: Boolean) = update { it[Keys.BOARD_ARROWS] = v }
    suspend fun setEngineDepth(v: Int) = update { it[Keys.ENGINE_DEPTH] = v.coerceIn(1, 5) }
    suspend fun setEngineMultiPv(v: Int) = update { it[Keys.ENGINE_MULTIPV] = v.coerceIn(1, 3) }
    suspend fun setOpeningRecognitionFlag(flag: String, enabled: Boolean) = update {
        when (flag) {
            "book" -> it[Keys.OPENING_BOOK] = enabled
            "opening" -> it[Keys.OPENING_RECOG] = enabled
            "gambit" -> it[Keys.GAMBIT_RECOG] = enabled
            "endgame" -> it[Keys.ENDGAME_RECOG] = enabled
        }
    }
    suspend fun setCoordinateNotation(v: Boolean) = update { it[Keys.COORD_NOTATION] = v }
    suspend fun setFontScale(v: Float) = update { it[Keys.FONT_SCALE] = v }
    suspend fun setLanguage(code: String) = update { it[Keys.LANGUAGE] = code }
    suspend fun setShowSocialFeed(v: Boolean) = update { it[Keys.SOCIAL_FEED] = v }
    suspend fun setAllowChatInGames(v: Boolean) = update { it[Keys.ALLOW_CHAT] = v }
    suspend fun setNotificationsEnabled(v: Boolean) = update { it[Keys.NOTIFICATIONS] = v }
    suspend fun setLocalAvatarId(id: String) = update { it[Keys.LOCAL_AVATAR] = id }
    suspend fun setLocalIdentity(name: String, bio: String) = update {
        it[Keys.LOCAL_NAME] = name.trim().take(36).ifBlank { "ChessHub Player" }
        it[Keys.LOCAL_BIO] = bio.trim().take(240)
    }
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

class LocalChessRepository(
    private val dao: ChessHubDao,
    private val puzzleDao: ChessPuzzleDao? = null,
    private val mediaDao: MediaAndNewsDao? = null,
) {
    fun observeGames(limit: Int = 100): Flow<List<LocalGameEntity>> = dao.observeRecentGames(limit)
    fun observeStudies(): Flow<List<LocalStudyEntity>> = dao.observeStudies()
    fun observeCollections(): Flow<List<CollectionEntity>> = dao.observeCollections()
    fun observePuzzleAttempts(): Flow<List<PuzzleAttemptEntity>> = dao.observePuzzleAttempts()
    fun observeCustomPuzzles(): Flow<List<CustomPuzzleEntity>> = dao.observeCustomPuzzles()
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>> = dao.observeLessonProgress()

    fun observePuzzleCollection(): Flow<List<ChessPuzzleEntity>> =
        puzzleDao?.observeAllPuzzles() ?: flowOf(emptyList())

    fun observeCheckmatePuzzles(): Flow<List<ChessPuzzleEntity>> =
        puzzleDao?.observeCheckmatePatterns() ?: flowOf(emptyList())

    fun observePuzzlesByRatingRange(minRating: Int, maxRating: Int): Flow<List<ChessPuzzleEntity>> =
        puzzleDao?.observeByDifficultyRange(minRating, maxRating) ?: flowOf(emptyList())

    fun observeBookmarkedPuzzles(): Flow<List<ChessPuzzleEntity>> =
        puzzleDao?.observeBookmarkedPuzzles() ?: flowOf(emptyList())

    fun observeMusicTracks(): Flow<List<MusicTrackEntity>> =
        mediaDao?.observeMusicTracks() ?: flowOf(emptyList())

    fun observeCachedNews(): Flow<List<CachedNewsEntity>> =
        mediaDao?.observeCachedNews() ?: flowOf(emptyList())

    suspend fun seedBuiltInPuzzlesIfNeeded() {
        val pDao = puzzleDao ?: return
        val entities = PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.map { pz ->
            ChessPuzzleEntity(
                id = pz.id,
                title = pz.title,
                fen = pz.fen,
                solutionMoves = pz.solutionUci.joinToString(" "),
                difficultyRating = pz.rating,
                themes = pz.themes.joinToString(","),
                hint = pz.hint,
                explanation = pz.explanation,
                isCheckmatePattern = pz.isCheckmatePattern,
                patternName = pz.patternName,
                source = "builtin",
            )
        }
        pDao.insertAllIgnoreConflict(entities)
    }

    suspend fun savePuzzle(puzzle: ChessPuzzleEntity) {
        puzzleDao?.upsertPuzzle(puzzle)
        if (puzzle.source == "custom" || puzzle.source == "mistake") {
            dao.upsertCustomPuzzle(
                CustomPuzzleEntity(
                    id = puzzle.id,
                    title = puzzle.title,
                    fen = puzzle.fen,
                    solutionUci = puzzle.solutionMoveList().firstOrNull() ?: puzzle.solutionMoves,
                    theme = puzzle.themeList().firstOrNull() ?: "Custom",
                    rating = puzzle.difficultyRating,
                ),
            )
        }
    }

    suspend fun togglePuzzleBookmark(id: String, bookmarked: Boolean) {
        puzzleDao?.setBookmarked(id, bookmarked)
    }

    suspend fun recordPuzzleSolveStats(id: String, solved: Boolean) {
        puzzleDao?.recordPuzzleAttemptStats(id, solved, System.currentTimeMillis())
    }

    suspend fun deletePuzzle(id: String) {
        puzzleDao?.deletePuzzleById(id)
    }

    suspend fun saveMusicTrack(track: MusicTrackEntity) {
        mediaDao?.upsertMusicTrack(track)
    }

    suspend fun deleteMusicTrack(id: String) {
        mediaDao?.deleteMusicTrack(id)
    }

    suspend fun saveCachedNews(items: List<CachedNewsEntity>) {
        mediaDao?.insertAllNews(items)
    }

    suspend fun clearNewsCache() {
        mediaDao?.clearCachedNews()
    }

    suspend fun saveGame(game: LocalGameEntity) = dao.upsertGame(game)
    suspend fun getGame(id: String): LocalGameEntity? = dao.getGameById(id)
    suspend fun deleteGame(id: String) = dao.deleteGame(id)
    suspend fun saveStudy(study: LocalStudyEntity) = dao.upsertStudy(study)
    suspend fun deleteStudy(id: String) = dao.deleteStudy(id)
    suspend fun saveCollection(col: CollectionEntity) = dao.upsertCollection(col)
    suspend fun deleteCollection(id: String) = dao.deleteCollection(id)
    suspend fun recordPuzzleAttempt(attempt: PuzzleAttemptEntity) = dao.insertPuzzleAttempt(attempt)
    suspend fun saveCustomPuzzle(puzzle: CustomPuzzleEntity) {
        dao.upsertCustomPuzzle(puzzle)
        puzzleDao?.upsertPuzzle(
            ChessPuzzleEntity(
                id = puzzle.id,
                title = puzzle.title,
                fen = puzzle.fen,
                solutionMoves = puzzle.solutionUci,
                difficultyRating = puzzle.rating,
                themes = puzzle.theme,
                hint = "Find the best continuation from your analyzed game.",
                explanation = "Created from your ChessHub analysis.",
                source = "mistake",
            ),
        )
    }
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
