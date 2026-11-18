package com.example.chess

/**
 * Standardized AI difficulty presets for local play.
 * Maps human-friendly difficulty tiers to search depth, centipawn noise,
 * opening book randomness, and representative bot profiles.
 */
enum class AiDifficulty(
    val displayName: String,
    val approxElo: Int,
    val searchDepth: Int,
    val randomnessCentipawns: Int,
    val aggressionWeight: Float,
    val bookRandomness: Float,
    val defaultBotId: String,
) {
    BEGINNER(
        displayName = "Beginner",
        approxElo = 450,
        searchDepth = 1,
        randomnessCentipawns = 180,
        aggressionWeight = 0.95f,
        bookRandomness = 0.80f,
        defaultBotId = "bot_novice",
    ),
    EASY(
        displayName = "Easy",
        approxElo = 850,
        searchDepth = 1,
        randomnessCentipawns = 100,
        aggressionWeight = 1.05f,
        bookRandomness = 0.60f,
        defaultBotId = "bot_improver",
    ),
    MEDIUM(
        displayName = "Medium",
        approxElo = 1220,
        searchDepth = 2,
        randomnessCentipawns = 55,
        aggressionWeight = 1.15f,
        bookRandomness = 0.42f,
        defaultBotId = "bot_tactical",
    ),
    HARD(
        displayName = "Hard",
        approxElo = 1750,
        searchDepth = 3,
        randomnessCentipawns = 18,
        aggressionWeight = 1.25f,
        bookRandomness = 0.25f,
        defaultBotId = "bot_sharpshooter",
    ),
    EXPERT(
        displayName = "Expert",
        approxElo = 2150,
        searchDepth = 4,
        randomnessCentipawns = 6,
        aggressionWeight = 1.12f,
        bookRandomness = 0.15f,
        defaultBotId = "bot_master",
    ),
    MASTER(
        displayName = "Master",
        approxElo = 2650,
        searchDepth = 4,
        randomnessCentipawns = 0,
        aggressionWeight = 1.08f,
        bookRandomness = 0.08f,
        defaultBotId = "bot_gm",
    );

    fun toBotProfile(): BotProfile =
        ChessHubBots.ALL.firstOrNull { it.id == defaultBotId } ?: BotProfile(
            id = "difficulty_${name.lowercase()}",
            name = "$displayName AI",
            approxStrengthLabel = "Approx. $approxElo",
            approxElo = approxElo,
            searchDepth = searchDepth,
            randomnessCentipawns = randomnessCentipawns,
            aggressionWeight = aggressionWeight,
            bookRandomness = bookRandomness,
            styleTag = displayName,
            bio = "Local engine preset tuned for $displayName strength.",
            preferredOpeningEco = "C50",
        )

    companion object {
        fun fromBotProfile(bot: BotProfile): AiDifficulty = when {
            bot.approxElo < 650 -> BEGINNER
            bot.approxElo < 1050 -> EASY
            bot.approxElo < 1550 -> MEDIUM
            bot.approxElo < 2000 -> HARD
            bot.approxElo < 2450 -> EXPERT
            else -> MASTER
        }
    }
}

/**
 * Result of validating a candidate chess move against the current position and variant rules.
 */
sealed interface MoveValidationResult {
    data class Valid(
        val resolvedMove: Move,
        val san: String,
        val nextState: ChessBoardState,
        val isCheck: Boolean,
        val gameStatus: GameStatus,
        val winner: PieceColor?,
    ) : MoveValidationResult

    data class Invalid(
        val reason: String,
    ) : MoveValidationResult
}

/**
 * Basic chess engine interface for local play, providing move validation,
 * legal move generation, position evaluation, and difficulty-scaled AI move selection.
 */
interface LocalChessEngine {
    val engineName: String

    fun legalMoves(state: ChessBoardState): List<Move>

    fun isMoveLegal(state: ChessBoardState, move: Move): Boolean

    fun validateMove(
        state: ChessBoardState,
        move: Move,
        positionCounts: Map<String, Int> = emptyMap(),
    ): MoveValidationResult

    fun validateUciMove(
        state: ChessBoardState,
        uci: String,
        positionCounts: Map<String, Int> = emptyMap(),
    ): MoveValidationResult

    suspend fun chooseMove(
        state: ChessBoardState,
        difficulty: AiDifficulty,
        playedUciMoves: List<String> = emptyList(),
        useOpeningBook: Boolean = true,
    ): Move?

    suspend fun chooseBotMove(
        state: ChessBoardState,
        bot: BotProfile,
        playedUciMoves: List<String> = emptyList(),
        useOpeningBook: Boolean = true,
    ): Move?

    suspend fun evaluateBestMove(
        state: ChessBoardState,
        difficulty: AiDifficulty = AiDifficulty.MEDIUM,
    ): EngineLine?

    fun explainPosition(state: ChessBoardState, bestLine: EngineLine?): String
}

/**
 * Pure-Kotlin implementation of [LocalChessEngine] backed by [MoveGenerator] and [ChessEngine].
 */
class KotlinChessEngineAdapter(
    private val lowMemoryMode: Boolean = false,
) : LocalChessEngine {
    private val searchEngine = ChessEngine(lowMemoryMode = lowMemoryMode)

    override val engineName: String = "ChessHub Kotlin Chess Engine 2.0"

    override fun legalMoves(state: ChessBoardState): List<Move> =
        MoveGenerator.legalMoves(state)

    override fun isMoveLegal(state: ChessBoardState, move: Move): Boolean =
        findMatchingLegalMove(MoveGenerator.legalMoves(state), move) != null

    override fun validateMove(
        state: ChessBoardState,
        move: Move,
        positionCounts: Map<String, Int>,
    ): MoveValidationResult {
        val currentResult = MoveGenerator.evaluateGameResult(state, positionCounts)
        if (currentResult.first.isTerminal) {
            return MoveValidationResult.Invalid("Game is already over (${currentResult.first.description}).")
        }

        val legal = MoveGenerator.legalMoves(state)
        val resolved = findMatchingLegalMove(legal, move)
            ?: return MoveValidationResult.Invalid("Illegal move ${move.uci} for ${state.variant.displayName}.")

        val san = MoveGenerator.toSan(state, resolved, legal)
        val nextState = MoveGenerator.applyMoveUnchecked(state, resolved)
        val nextKey = nextState.positionKey()
        val updatedCounts = if (positionCounts.isEmpty()) {
            emptyMap()
        } else {
            positionCounts + (nextKey to ((positionCounts[nextKey] ?: 0) + 1))
        }
        val (status, winner) = MoveGenerator.evaluateGameResult(nextState, updatedCounts)
        val isCheck = MoveGenerator.isKingInCheck(nextState, nextState.sideToMove)

        return MoveValidationResult.Valid(
            resolvedMove = resolved,
            san = san,
            nextState = nextState,
            isCheck = isCheck,
            gameStatus = status,
            winner = winner,
        )
    }

    override fun validateUciMove(
        state: ChessBoardState,
        uci: String,
        positionCounts: Map<String, Int>,
    ): MoveValidationResult {
        val normalized = uci.trim()
        if (normalized.length < 4) {
            return MoveValidationResult.Invalid("Invalid UCI move format: '$uci'.")
        }
        val legal = MoveGenerator.legalMoves(state)
        val matched = legal.firstOrNull { it.uci.equals(normalized, ignoreCase = true) }
            ?: return MoveValidationResult.Invalid("Illegal UCI move '$uci' in current position.")
        return validateMove(state, matched, positionCounts)
    }

    override suspend fun chooseMove(
        state: ChessBoardState,
        difficulty: AiDifficulty,
        playedUciMoves: List<String>,
        useOpeningBook: Boolean,
    ): Move? = searchEngine.chooseBotMove(
        state = state,
        bot = difficulty.toBotProfile(),
        playedUciMoves = playedUciMoves,
        useOpeningBook = useOpeningBook,
    )

    override suspend fun chooseBotMove(
        state: ChessBoardState,
        bot: BotProfile,
        playedUciMoves: List<String>,
        useOpeningBook: Boolean,
    ): Move? = searchEngine.chooseBotMove(
        state = state,
        bot = bot,
        playedUciMoves = playedUciMoves,
        useOpeningBook = useOpeningBook,
    )

    override suspend fun evaluateBestMove(
        state: ChessBoardState,
        difficulty: AiDifficulty,
    ): EngineLine? {
        val timeLimit = when (difficulty) {
            AiDifficulty.BEGINNER, AiDifficulty.EASY -> 350L
            AiDifficulty.MEDIUM -> 650L
            AiDifficulty.HARD -> 900L
            AiDifficulty.EXPERT, AiDifficulty.MASTER -> 1200L
        }
        return searchEngine.evaluateBest(
            state = state,
            depth = difficulty.searchDepth,
            timeLimitMs = timeLimit,
        )
    }

    override fun explainPosition(state: ChessBoardState, bestLine: EngineLine?): String =
        searchEngine.explainCurrentPosition(state, bestLine)

    private fun findMatchingLegalMove(legal: List<Move>, candidate: Move): Move? {
        if (candidate.dropPieceType != null) {
            return legal.firstOrNull {
                it.dropPieceType == candidate.dropPieceType && it.to == candidate.to
            }
        }
        return legal.firstOrNull {
            it.dropPieceType == null &&
                it.from == candidate.from &&
                it.to == candidate.to &&
                (it.promotion == candidate.promotion || (candidate.promotion == null && it.promotion == PieceType.QUEEN))
        }
    }
}
