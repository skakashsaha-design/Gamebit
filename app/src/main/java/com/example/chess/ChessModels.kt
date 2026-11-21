package com.example.chess

import kotlin.math.abs

enum class PieceColor {
    WHITE,
    BLACK;

    val opposite: PieceColor
        get() = if (this == WHITE) BLACK else WHITE

    val fenChar: Char
        get() = if (this == WHITE) 'w' else 'b'
}

enum class PieceType(val fenSymbol: Char, val sanLetter: String, val materialValue: Int) {
    PAWN('p', "", 100),
    KNIGHT('n', "N", 320),
    BISHOP('b', "B", 330),
    ROOK('r', "R", 500),
    QUEEN('q', "Q", 900),
    KING('k', "K", 20000);

    companion object {
        fun fromChar(c: Char): PieceType? = when (c.lowercaseChar()) {
            'p' -> PAWN
            'n' -> KNIGHT
            'b' -> BISHOP
            'r' -> ROOK
            'q' -> QUEEN
            'k' -> KING
            else -> null
        }
    }
}

data class Piece(val type: PieceType, val color: PieceColor) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.fenSymbol.uppercaseChar() else type.fenSymbol.lowercaseChar()

    val accessibleName: String
        get() = "${if (color == PieceColor.WHITE) "White" else "Black"} ${type.name.lowercase().replaceFirstChar { it.uppercase() }}"

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(type, color)
        }
    }
}

@JvmInline
value class Square(val index: Int) {
    val file: Int get() = index and 7 // 0..7 (a..h)
    val rank: Int get() = index shr 3 // 0..7 (1..8)

    val algebraic: String
        get() = "${('a' + file)}${('1' + rank)}"

    val isLight: Boolean
        get() = ((file + rank) and 1) == 1

    fun distanceTo(other: Square): Int = maxOf(abs(file - other.file), abs(rank - other.rank))

    companion object {
        fun of(file: Int, rank: Int): Square {
            require(file in 0..7 && rank in 0..7) { "Invalid square ($file, $rank)" }
            return Square((rank shl 3) or file)
        }

        fun fromAlgebraic(str: String): Square? {
            if (str.length != 2) return null
            val f = str[0].lowercaseChar() - 'a'
            val r = str[1] - '1'
            if (f !in 0..7 || r !in 0..7) return null
            return of(f, r)
        }
    }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val dropPieceType: PieceType? = null,
) {
    val uci: String
        get() = if (dropPieceType != null) {
            "${dropPieceType.fenSymbol.uppercaseChar()}@${to.algebraic}"
        } else {
            val promo = promotion?.fenSymbol?.lowercaseChar()?.toString().orEmpty()
            "${from.algebraic}${to.algebraic}$promo"
        }
}

enum class ChessVariant(
    val displayName: String,
    val shortCode: String,
    val description: String,
) {
    STANDARD("Standard", "standard", "Classic FIDE chess rules"),
    CHESS960("Chess960", "chess960", "Fischer Random starting back rank"),
    THREE_CHECK("Three-Check", "threeCheck", "Win by checking the opponent 3 times"),
    KING_OF_THE_HILL("King of the Hill", "kingOfTheHill", "Win by reaching e4, d4, e5, or d5 with your King"),
    ANTICHESS("Antichess", "antichess", "Captures are forced; lose all pieces or stalemate to win"),
    ATOMIC("Atomic", "atomic", "Captures explode surrounding non-pawn pieces"),
    HORDE("Horde", "horde", "White has 36 pawns against Black's standard army"),
    RACING_KINGS("Racing Kings", "racingKings", "First King to reach the 8th rank wins; no checks allowed"),
    CRAZYHOUSE("Crazyhouse", "crazyhouse", "Captured pieces join your pocket and can be dropped onto the board"),
}

enum class ClockType(val label: String) {
    FISCHER("Fischer Increment"),
    BRONSTEIN("Bronstein Delay"),
    SIMPLE_DELAY("Simple Delay"),
    CORRESPONDENCE("Correspondence"),
    UNLIMITED("Unlimited"),
}

data class TimeControl(
    val label: String,
    val initialSeconds: Int,
    val incrementSeconds: Int = 0,
    val delaySeconds: Int = 0,
    val clockType: ClockType = ClockType.FISCHER,
) {
    val category: String
        get() = when {
            clockType == ClockType.UNLIMITED -> "unlimited"
            clockType == ClockType.CORRESPONDENCE -> "correspondence"
            initialSeconds + incrementSeconds * 40 < 180 -> "bullet"
            initialSeconds + incrementSeconds * 40 < 600 -> "blitz"
            initialSeconds + incrementSeconds * 40 < 1800 -> "rapid"
            else -> "classical"
        }

    companion object {
        val PRESETS = listOf(
            TimeControl("1+0", 60, 0),
            TimeControl("2+1", 120, 1),
            TimeControl("3+0", 180, 0),
            TimeControl("3+2", 180, 2),
            TimeControl("5+0", 300, 0),
            TimeControl("5+3", 300, 3),
            TimeControl("10+0", 600, 0),
            TimeControl("10+5", 600, 5),
            TimeControl("15+10", 900, 10),
            TimeControl("30+0", 1800, 0),
            TimeControl("5+3d", 300, 0, 3, ClockType.SIMPLE_DELAY),
            TimeControl("5+3b", 300, 0, 3, ClockType.BRONSTEIN),
            TimeControl("3 Days", 259200, 0, 0, ClockType.CORRESPONDENCE),
            TimeControl("Unlimited", 0, 0, 0, ClockType.UNLIMITED),
        )
    }
}

enum class GameStatus(val isTerminal: Boolean, val description: String) {
    IN_PROGRESS(false, "In Progress"),
    CHECKMATE(true, "Checkmate"),
    STALEMATE(true, "Stalemate"),
    INSUFFICIENT_MATERIAL(true, "Insufficient Material"),
    THREEFOLD_REPETITION(true, "Threefold Repetition"),
    FIVEFOLD_REPETITION(true, "Fivefold Repetition"),
    FIFTY_MOVE_RULE(true, "50-Move Rule"),
    SEVENTY_FIVE_MOVE_RULE(true, "75-Move Rule"),
    DRAW_BY_AGREEMENT(true, "Draw by Agreement"),
    RESIGNATION(true, "Resignation"),
    TIMEOUT(true, "Time Out"),
    ABORTED(true, "Game Aborted"),
    VARIANT_WIN(true, "Variant Win Condition"),
}

data class CastlingRights(
    val whiteKingSideFile: Int? = 7,
    val whiteQueenSideFile: Int? = 0,
    val blackKingSideFile: Int? = 7,
    val blackQueenSideFile: Int? = 0,
) {
    fun toFenString(isChess960: Boolean): String {
        val sb = StringBuilder()
        if (isChess960) {
            whiteKingSideFile?.let { sb.append('A' + it) }
            whiteQueenSideFile?.let { sb.append('A' + it) }
            blackKingSideFile?.let { sb.append('a' + it) }
            blackQueenSideFile?.let { sb.append('a' + it) }
        } else {
            if (whiteKingSideFile != null) sb.append('K')
            if (whiteQueenSideFile != null) sb.append('Q')
            if (blackKingSideFile != null) sb.append('k')
            if (blackQueenSideFile != null) sb.append('q')
        }
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        val NONE = CastlingRights(null, null, null, null)
    }
}

enum class BotVoiceStyle(val label: String) {
    WARM("Warm"),
    PLAYFUL("Playful"),
    CASUAL("Casual"),
    SHARP("Sharp"),
    CALM("Calm"),
    FIERY("Fiery"),
    STOIC("Stoic"),
    ANALYTICAL("Analytical"),
    GRANDMASTER("Grandmaster"),
    SYNTHETIC("Synthetic"),
}

data class BotVoiceProfile(
    val pitch: Float = 1.0f,
    val rate: Float = 1.0f,
    val timbreSlot: Int = 0,
    val voiceStyle: BotVoiceStyle = BotVoiceStyle.CASUAL,
    val preferredLocaleTag: String = "en-US",
)

data class BotAvatarSpec(
    val primaryHex: Long = 0xFF1E293B,
    val secondaryHex: Long = 0xFF0F172A,
    val accentHex: Long = 0xFF10B981,
    val emblemStyle: String = "classic",
    val badgePiece: PieceType = PieceType.KNIGHT,
)

data class BotProfile(
    val id: String,
    val name: String,
    val approxStrengthLabel: String,
    val approxElo: Int,
    val searchDepth: Int,
    val randomnessCentipawns: Int,
    val aggressionWeight: Float,
    val bookRandomness: Float,
    val styleTag: String,
    val bio: String,
    val preferredOpeningEco: String,
    val voiceProfile: BotVoiceProfile = BotVoiceProfile(),
    val avatarSpec: BotAvatarSpec = BotAvatarSpec(),
) {
    fun dialogueFor(
        event: String,
        moveCount: Int,
        san: String? = null,
        openingName: String? = null,
        capturedPiece: PieceType? = null,
    ): String = when (event) {
        "start" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.WARM -> "Hey! I'm $name ($approxStrengthLabel). Take your time and let's enjoy some chess!"
            BotVoiceStyle.PLAYFUL -> "Yo, $name here! Ready for wild tactics and surprises?"
            BotVoiceStyle.SHARP -> "$name online. Watch every diagonal—I'm hunting for forks from move one!"
            BotVoiceStyle.CALM -> "Greetings, I am $name. Let's build a clean strategic battle ($preferredOpeningEco)."
            BotVoiceStyle.FIERY -> "$name stepping up! Don't blink—I love launching storms against the king!"
            BotVoiceStyle.STOIC -> "$name here. Patience and structure decide games."
            BotVoiceStyle.ANALYTICAL -> "I'm $name. Let's test concrete calculation and deep positional nuances."
            BotVoiceStyle.GRANDMASTER -> "$name at the board. Precision and timing will decide everything today."
            BotVoiceStyle.SYNTHETIC -> "Apex-CH initialized. Depth prioritized. Calculating optimal continuations."
            BotVoiceStyle.CASUAL -> "Hi! I'm $name ($approxStrengthLabel). Let's play a crisp, fun match!"
        }
        "opening" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.FIERY -> "${openingName ?: preferredOpeningEco}! Love this sharp battleground—time to attack."
            BotVoiceStyle.CALM, BotVoiceStyle.STOIC -> "Ah, the ${openingName ?: preferredOpeningEco}. Solid ground for maneuvering."
            BotVoiceStyle.SYNTHETIC -> "Opening signature detected: ${openingName ?: preferredOpeningEco}. Book lines aligned."
            else -> "Classic ${openingName ?: preferredOpeningEco}! Let's see who handles the middlegame plan better."
        }
        "capture" -> {
            val pieceWord = capturedPiece?.name?.lowercase() ?: "piece"
            when (voiceProfile.voiceStyle) {
                BotVoiceStyle.PLAYFUL -> "Ooh, snatching that $pieceWord cleanly!"
                BotVoiceStyle.FIERY -> "Taking your $pieceWord (${san ?: "capture"}) and keeping the initiative!"
                BotVoiceStyle.STOIC, BotVoiceStyle.CALM -> "Trading the $pieceWord off to clarify the structure."
                BotVoiceStyle.SYNTHETIC -> "Material gain verified on ${san ?: "capture"}. Advantage updated."
                else -> "Clean exchange—let's see what opens up next."
            }
        }
        "check" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.FIERY, BotVoiceStyle.SHARP -> "Check with ${san ?: "tempo"}! Nowhere comfortable to hide!"
            BotVoiceStyle.SYNTHETIC -> "Forcing check delivered: ${san ?: "move"}. King safety compromised."
            BotVoiceStyle.WARM, BotVoiceStyle.CASUAL -> "Check! Better tuck your king somewhere safe."
            else -> "Check (${san ?: "tempo"}). Let's test your defensive accuracy."
        }
        "castled" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.CALM, BotVoiceStyle.STOIC -> "King tucked away safely. Now the real middlegame begins."
            BotVoiceStyle.FIERY -> "Castling first, launching pawns next!"
            else -> "King is safe—time to connect the rooks."
        }
        "promotion" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.SYNTHETIC -> "Pawn promotion executed: ${san ?: "new piece"}. Heavy firepower online."
            else -> "A brand-new piece enters the board! Things just got intense."
        }
        "endgame" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.STOIC, BotVoiceStyle.ANALYTICAL -> "Endgame reached. King activity and passed pawns rule from here."
            else -> "Queens off, endgame on! Every single tempo matters now."
        }
        "blunder_opp" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.PLAYFUL -> "Wait—did you really mean to leave that square open?!"
            BotVoiceStyle.SHARP, BotVoiceStyle.FIERY -> "Aha! That slip gives me a real tactical shot!"
            BotVoiceStyle.SYNTHETIC -> "Tactical inaccuracy detected. Converting advantage."
            BotVoiceStyle.WARM -> "Careful there! Let me show you why that square is tricky."
            else -> "Hmm, that move left a soft spot—let's test it!"
        }
        "great_opp" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.WARM, BotVoiceStyle.CASUAL -> "Whoa, brilliant move! That caught me off guard!"
            BotVoiceStyle.GRANDMASTER -> "Impressive find—that puts serious pressure on my position."
            BotVoiceStyle.SYNTHETIC -> "High-precision candidate found by opponent. Adjusting search."
            else -> "Sharp play! You spotted a strong resource there."
        }
        "win" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.WARM -> "Good game! Tap Game Review if you want to explore the turning points together."
            BotVoiceStyle.SYNTHETIC -> "Match concluded. Run Game Review for full accuracy telemetry."
            else -> "Well fought! Check the Evaluation Graph to see where the tide turned."
        }
        "lose" -> when (voiceProfile.voiceStyle) {
            BotVoiceStyle.FIERY -> "Respect! You out-tacticked me fair and square—rematch?"
            BotVoiceStyle.SYNTHETIC -> "Defeat logged. Your accuracy outperformed current depth."
            else -> "Well played! You broke through cleanly—great technique."
        }
        "draw" -> "Balanced fight! A hard-earned draw by both sides."
        else -> when {
            moveCount < 10 -> "Developing toward central control and king safety."
            voiceProfile.voiceStyle == BotVoiceStyle.SYNTHETIC -> "Evaluating candidate lines at depth $searchDepth."
            else -> "Scanning tactics and improving my worst-placed piece..."
        }
    }
}

object ChessHubBots {
    val ALL = listOf(
        BotProfile(
            "bot_sprout", "Nadia", "Approx. 250 Bot", 250, 1, 240, 0.8f, 0.9f, "Beginner",
            "Learns alongside you and frequently leaves tactical chances.", "A00",
            BotVoiceProfile(pitch = 1.20f, rate = 0.95f, timbreSlot = 1, voiceStyle = BotVoiceStyle.WARM, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFF065F46, 0xFF064E3B, 0xFF34D399, "sprout", PieceType.PAWN),
        ),
        BotProfile(
            "bot_novice", "Leo", "Approx. 450 Bot", 450, 1, 180, 0.95f, 0.8f, "Novice",
            "Likes early queen sorties and captures hanging pawns.", "B00",
            BotVoiceProfile(pitch = 1.12f, rate = 1.09f, timbreSlot = 0, voiceStyle = BotVoiceStyle.PLAYFUL, preferredLocaleTag = "en-AU"),
            BotAvatarSpec(0xFFB45309, 0xFF78350F, 0xFFFBBF24, "sparks", PieceType.QUEEN),
        ),
        BotProfile(
            "bot_tempo", "Tariq", "Approx. 650 Bot", 650, 1, 130, 1.0f, 0.7f, "Casual",
            "Loves developing knights early and trading pieces.", "C20",
            BotVoiceProfile(pitch = 0.96f, rate = 1.02f, timbreSlot = 2, voiceStyle = BotVoiceStyle.CASUAL, preferredLocaleTag = "en-GB"),
            BotAvatarSpec(0xFF0369A1, 0xFF0C4A6E, 0xFF38BDF8, "helm", PieceType.KNIGHT),
        ),
        BotProfile(
            "bot_improver", "Maya", "Approx. 850 Bot", 850, 1, 100, 1.05f, 0.6f, "Improver",
            "Castles early and follows basic central development.", "C50",
            BotVoiceProfile(pitch = 1.16f, rate = 1.03f, timbreSlot = 3, voiceStyle = BotVoiceStyle.WARM, preferredLocaleTag = "en-IN"),
            BotAvatarSpec(0xFF6D28D9, 0xFF4C1D95, 0xFFC084FC, "aurora", PieceType.BISHOP),
        ),
        BotProfile(
            "bot_club", "Sadia", "Approx. 1050 Bot", 1050, 2, 75, 1.1f, 0.5f, "Tactical",
            "Spots basic forks, skewers, and back-rank mates reliably.", "C44",
            BotVoiceProfile(pitch = 1.10f, rate = 1.07f, timbreSlot = 5, voiceStyle = BotVoiceStyle.SHARP, preferredLocaleTag = "en-IN"),
            BotAvatarSpec(0xFFBE185D, 0xFF831843, 0xFFF472B6, "tiara", PieceType.KNIGHT),
        ),
        BotProfile(
            "bot_tactical", "Victor", "Approx. 1220 Bot", 1220, 2, 55, 1.2f, 0.42f, "Tactical",
            "Enjoys open files and double attacks on unguarded targets.", "B21",
            BotVoiceProfile(pitch = 0.87f, rate = 1.06f, timbreSlot = 4, voiceStyle = BotVoiceStyle.SHARP, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFF991B1B, 0xFF7F1D1D, 0xFFF87171, "bolt", PieceType.ROOK),
        ),
        BotProfile(
            "bot_positional", "Henrik", "Approx. 1400 Bot", 1400, 2, 35, 0.9f, 0.35f, "Positional",
            "Solid center control, London System and Queen's Gambit specialist.", "D02",
            BotVoiceProfile(pitch = 0.83f, rate = 0.92f, timbreSlot = 6, voiceStyle = BotVoiceStyle.CALM, preferredLocaleTag = "en-GB"),
            BotAvatarSpec(0xFF1E3A8A, 0xFF172554, 0xFF60A5FA, "glasses", PieceType.BISHOP),
        ),
        BotProfile(
            "bot_solid", "Elena", "Approx. 1550 Bot", 1550, 2, 26, 0.92f, 0.30f, "Defensive",
            "Patient Slav and Caro-Kann defender who punishes overextension.", "B10",
            BotVoiceProfile(pitch = 1.07f, rate = 0.94f, timbreSlot = 7, voiceStyle = BotVoiceStyle.CALM, preferredLocaleTag = "en-GB"),
            BotAvatarSpec(0xFF0F766E, 0xFF134E4A, 0xFF2DD4BF, "shield", PieceType.ROOK),
        ),
        BotProfile(
            "bot_sharpshooter", "Mateo", "Approx. 1750 Bot", 1750, 3, 18, 1.3f, 0.25f, "Aggressive",
            "Pushes kingside attacks, Evans Gambit, and sharp Sicilian lines.", "B90",
            BotVoiceProfile(pitch = 1.01f, rate = 1.16f, timbreSlot = 8, voiceStyle = BotVoiceStyle.FIERY, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFFC2410C, 0xFF7C2D12, 0xFFFB923C, "flame", PieceType.KNIGHT),
        ),
        BotProfile(
            "bot_sharp", "Sofia", "Approx. 1900 Bot", 1900, 3, 14, 1.28f, 0.22f, "Sharp",
            "Plays imbalanced Benoni and King's Indian counterattacks.", "E60",
            BotVoiceProfile(pitch = 1.15f, rate = 1.12f, timbreSlot = 9, voiceStyle = BotVoiceStyle.FIERY, preferredLocaleTag = "en-AU"),
            BotAvatarSpec(0xFFA21CAF, 0xFF701A75, 0xFFE879F9, "diamond", PieceType.QUEEN),
        ),
        BotProfile(
            "bot_defender", "Kenji", "Approx. 1980 Bot", 1980, 3, 10, 0.85f, 0.20f, "Defensive",
            "Neutralizes gambits with accurate consolidation.", "D30",
            BotVoiceProfile(pitch = 0.84f, rate = 0.90f, timbreSlot = 10, voiceStyle = BotVoiceStyle.STOIC, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFF334155, 0xFF0F172A, 0xFF94A3B8, "kabuto", PieceType.ROOK),
        ),
        BotProfile(
            "bot_balanced", "Clara", "Approx. 2050 Bot", 2050, 3, 8, 1.02f, 0.18f, "Balanced",
            "Classical Ruy Lopez & Catalan player balancing strategy and tactics.", "E00",
            BotVoiceProfile(pitch = 1.05f, rate = 0.98f, timbreSlot = 11, voiceStyle = BotVoiceStyle.ANALYTICAL, preferredLocaleTag = "en-GB"),
            BotAvatarSpec(0xFF4338CA, 0xFF312E81, 0xFFA5B4FC, "laurel", PieceType.QUEEN),
        ),
        BotProfile(
            "bot_master", "Aarav", "Approx. 2150 Bot", 2150, 4, 6, 1.12f, 0.15f, "Master",
            "Deep calculation and disciplined positional squeezing.", "C88",
            BotVoiceProfile(pitch = 0.90f, rate = 0.96f, timbreSlot = 12, voiceStyle = BotVoiceStyle.ANALYTICAL, preferredLocaleTag = "en-IN"),
            BotAvatarSpec(0xFF047857, 0xFF064E3B, 0xFF10B981, "monocle", PieceType.BISHOP),
        ),
        BotProfile(
            "bot_endgame", "Soren", "Approx. 2250 Bot", 2250, 4, 4, 0.95f, 0.12f, "Endgame",
            "Relentlessly trades into favorable rook and pawn endings.", "D37",
            BotVoiceProfile(pitch = 0.80f, rate = 0.88f, timbreSlot = 13, voiceStyle = BotVoiceStyle.STOIC, preferredLocaleTag = "en-GB"),
            BotAvatarSpec(0xFF3F3F46, 0xFF18181B, 0xFFE4E4E7, "hood", PieceType.KING),
        ),
        BotProfile(
            "bot_im", "Ananya", "Approx. 2450 Bot", 2450, 4, 3, 1.10f, 0.10f, "Master",
            "International Master style tactical precision and theory.", "B96",
            BotVoiceProfile(pitch = 1.09f, rate = 1.02f, timbreSlot = 14, voiceStyle = BotVoiceStyle.GRANDMASTER, preferredLocaleTag = "en-IN"),
            BotAvatarSpec(0xFF0E7490, 0xFF164E63, 0xFF22D3EE, "crown", PieceType.QUEEN),
        ),
        BotProfile(
            "bot_gm", "Vikram", "Approx. 2650 Bot", 2650, 4, 1, 1.15f, 0.08f, "Grandmaster",
            "Combines dynamic sacrifices with rock-solid structure.", "C89",
            BotVoiceProfile(pitch = 0.85f, rate = 0.97f, timbreSlot = 15, voiceStyle = BotVoiceStyle.GRANDMASTER, preferredLocaleTag = "en-IN"),
            BotAvatarSpec(0xFF92400E, 0xFF451A03, 0xFFF59E0B, "crown", PieceType.KING),
        ),
        BotProfile(
            "bot_supergm", "Ren", "Approx. 2850 Bot", 2850, 5, 0, 1.08f, 0.06f, "Grandmaster",
            "Relentless initiative and razor-sharp multi-move tactics.", "C78",
            BotVoiceProfile(pitch = 0.93f, rate = 1.06f, timbreSlot = 16, voiceStyle = BotVoiceStyle.GRANDMASTER, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFF7F1D1D, 0xFF450A0A, 0xFFFCA5A5, "laurel", PieceType.KING),
        ),
        BotProfile(
            "bot_apex", "Apex-CH", "Approx. 3000 Bot", 3000, 5, 0, 1.05f, 0.03f, "Engine",
            "Maximum local Kotlin alpha-beta depth with minimal randomness.", "E90",
            BotVoiceProfile(pitch = 0.76f, rate = 1.18f, timbreSlot = 17, voiceStyle = BotVoiceStyle.SYNTHETIC, preferredLocaleTag = "en-US"),
            BotAvatarSpec(0xFF0F172A, 0xFF020617, 0xFF06B6D4, "cyber", PieceType.KING),
        ),
    )
}

data class CountryInfo(
    val isoCode: String,
    val name: String,
    val flagEmoji: String,
    val defaultFideCode: String,
)

object CountryCatalog {
    val FIDE_FEDERATIONS = listOf(
        "AFG", "ALB", "ALG", "AND", "ANG", "ANT", "ARG", "ARM", "ARU", "AUS", "AUT", "AZE",
        "BAH", "BAN", "BAR", "BDI", "BEL", "BEN", "BER", "BHU", "BIH", "BLR", "BOL", "BOT",
        "BRA", "BRN", "BRU", "BUL", "BUR", "CAF", "CAM", "CAN", "CAY", "CGO", "CHA", "CHI",
        "CHN", "CIV", "CMR", "COD", "COL", "COM", "CPV", "CRC", "CRO", "CUB", "CYP", "CZE",
        "DEN", "DJI", "DMA", "DOM", "ECU", "EGY", "ENG", "ESA", "ESP", "EST", "ETH", "FAI",
        "FIJ", "FIN", "FRA", "GAB", "GAM", "GEO", "GER", "GHA", "GRE", "GRN", "GUA", "GUM",
        "GUY", "HAI", "HKG", "HON", "HUN", "INA", "IND", "IRI", "IRL", "IRQ", "ISL", "ISR",
        "ISV", "ITA", "JAM", "JCI", "JOR", "JPN", "KAZ", "KEN", "KGZ", "KOR", "KOS", "KSA",
        "KUW", "LAO", "LAT", "LBA", "LBN", "LBR", "LCA", "LES", "LIE", "LTU", "LUX", "MAC",
        "MAD", "MAR", "MAS", "MAW", "MDA", "MDV", "MEX", "MGL", "MKD", "MLI", "MLT", "MNC",
        "MNE", "MOZ", "MRI", "MTN", "MYA", "NAM", "NCA", "NED", "NEP", "NGR", "NIG", "NOR",
        "NZL", "OMA", "PAK", "PAN", "PAR", "PER", "PHI", "PLE", "PLW", "PNG", "POL", "POR",
        "PUR", "QAT", "ROU", "RSA", "RUS", "RWA", "SCO", "SEN", "SEY", "SGP", "SKN", "SLE",
        "SLO", "SMR", "SOL", "SOM", "SRB", "SRI", "SSD", "STP", "SUD", "SUI", "SUR", "SVK",
        "SWE", "SWZ", "SYR", "TAN", "TGA", "THA", "TJK", "TKM", "TLS", "TOG", "TPE", "TTO",
        "TUN", "TUR", "UAE", "UGA", "UKR", "URU", "USA", "UZB", "VEN", "VIE", "VIN", "WLS",
        "YEM", "ZAM", "ZIM", "FID",
    )

    // Full ISO-3166-1 country list generated programmatically from java.util.Locale + FIDE mapping
    val ALL_COUNTRIES: List<CountryInfo> by lazy {
        val fideOverrides = mapOf(
            "BD" to "BAN", "GB" to "ENG", "DE" to "GER", "IN" to "IND", "US" to "USA",
            "FR" to "FRA", "ES" to "ESP", "RU" to "FID", "CN" to "CHN", "NO" to "NOR",
            "NL" to "NED", "PL" to "POL", "UA" to "UKR", "UZ" to "UZB", "AM" to "ARM",
            "AZ" to "AZE", "HU" to "HUN", "IT" to "ITA", "BR" to "BRA", "AR" to "ARG",
            "CA" to "CAN", "AU" to "AUS", "JP" to "JPN", "KR" to "KOR", "VN" to "VIE",
            "PH" to "PHI", "ID" to "INA", "MY" to "MAS", "SG" to "SGP", "PK" to "PAK",
            "LK" to "SRI", "NP" to "NEP", "TR" to "TUR", "IR" to "IRI", "EG" to "EGY",
            "ZA" to "RSA", "NG" to "NGR", "SE" to "SWE", "DK" to "DEN", "FI" to "FIN",
            "CH" to "SUI", "AT" to "AUT", "BE" to "BEL", "PT" to "POR", "GR" to "GRE",
            "RO" to "ROU", "BG" to "BUL", "RS" to "SRB", "HR" to "CRO", "CZ" to "CZE",
            "SK" to "SVK", "GE" to "GEO", "KZ" to "KAZ", "IL" to "ISR", "MX" to "MEX",
            "CO" to "COL", "PE" to "PER", "CL" to "CHI", "CU" to "CUB", "NZ" to "NZL",
        )
        java.util.Locale.getISOCountries().map { iso ->
            val locale = java.util.Locale("", iso)
            val name = locale.getDisplayCountry(java.util.Locale.ENGLISH).ifBlank { iso }
            val firstChar = Character.codePointAt(iso, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(iso, 1) - 0x41 + 0x1F1E6
            val flag = String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
            val fide = fideOverrides[iso] ?: (iso + "A").take(3)
            CountryInfo(isoCode = iso, name = name, flagEmoji = flag, defaultFideCode = fide)
        }.sortedBy { it.name }
    }

    fun flagForIso(iso: String): String =
        ALL_COUNTRIES.firstOrNull { it.isoCode.equals(iso, ignoreCase = true) }?.flagEmoji ?: "🏳️"
}
