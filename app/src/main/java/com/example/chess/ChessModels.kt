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
)

object ChessHubBots {
    val ALL = listOf(
        BotProfile("bot_sprout", "Nadia", "Approx. 250 Bot", 250, 1, 240, 0.8f, 0.9f, "Beginner", "Learns alongside you and frequently leaves tactical chances.", "A00"),
        BotProfile("bot_tempo", "Tariq", "Approx. 650 Bot", 650, 1, 130, 1.0f, 0.7f, "Casual", "Loves developing knights early and trading queens.", "C20"),
        BotProfile("bot_club", "Sadia", "Approx. 1050 Bot", 1050, 2, 75, 1.1f, 0.5f, "Tactical", "Spots basic forks and back-rank mates reliably.", "C50"),
        BotProfile("bot_positional", "Henrik", "Approx. 1400 Bot", 1400, 2, 35, 0.9f, 0.35f, "Positional", "Solid center control, London System and Queen's Gambit specialist.", "D02"),
        BotProfile("bot_sharpshooter", "Mateo", "Approx. 1750 Bot", 1750, 3, 18, 1.3f, 0.25f, "Aggressive", "Pushes kingside attacks and sharp Sicilian lines.", "B90"),
        BotProfile("bot_master", "Aarav", "Approx. 2100 Bot", 2100, 4, 6, 1.15f, 0.15f, "Master", "Deep calculation and disciplined endgame technique.", "C88"),
        BotProfile("bot_apex", "Apex-CH", "Approx. 2400 Bot", 2400, 5, 0, 1.05f, 0.05f, "Engine", "Maximum local search depth with minimal randomness.", "E60"),
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
