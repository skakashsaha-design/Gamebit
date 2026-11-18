package com.example.chess

import kotlin.random.Random

data class ChessBoardState(
    val board: Array<Piece?> = arrayOfNulls(64),
    val sideToMove: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantSquare: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val variant: ChessVariant = ChessVariant.STANDARD,
    val whiteChecksRemaining: Int = 3,
    val blackChecksRemaining: Int = 3,
    val whitePocket: Map<PieceType, Int> = emptyMap(),
    val blackPocket: Map<PieceType, Int> = emptyMap(),
    val promotedSquares: Set<Int> = emptySet(),
) {
    fun pieceAt(square: Square): Piece? = board[square.index]
    fun pieceAt(file: Int, rank: Int): Piece? = board[(rank shl 3) or file]

    fun positionKey(): String {
        val sb = StringBuilder(80)
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = pieceAt(file, rank)
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(' ').append(sideToMove.fenChar)
        sb.append(' ').append(castlingRights.toFenString(variant == ChessVariant.CHESS960))
        sb.append(' ').append(enPassantSquare?.algebraic ?: "-")
        if (variant == ChessVariant.THREE_CHECK) {
            sb.append(" +${3 - blackChecksRemaining}+${3 - whiteChecksRemaining}")
        }
        if (variant == ChessVariant.CRAZYHOUSE) {
            sb.append(" [")
            whitePocket.forEach { (t, c) -> repeat(c) { sb.append(t.fenSymbol.uppercaseChar()) } }
            blackPocket.forEach { (t, c) -> repeat(c) { sb.append(t.fenSymbol.lowercaseChar()) } }
            sb.append(']')
        }
        return sb.toString()
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = pieceAt(file, rank)
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                    if (variant == ChessVariant.CRAZYHOUSE && ((rank shl 3) or file) in promotedSquares) {
                        sb.append('~')
                    }
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        if (variant == ChessVariant.CRAZYHOUSE && (whitePocket.isNotEmpty() || blackPocket.isNotEmpty())) {
            sb.append('[')
            whitePocket.forEach { (t, c) -> repeat(c) { sb.append(t.fenSymbol.uppercaseChar()) } }
            blackPocket.forEach { (t, c) -> repeat(c) { sb.append(t.fenSymbol.lowercaseChar()) } }
            sb.append(']')
        }
        sb.append(' ').append(sideToMove.fenChar)
        sb.append(' ').append(castlingRights.toFenString(variant == ChessVariant.CHESS960))
        sb.append(' ').append(enPassantSquare?.algebraic ?: "-")
        sb.append(' ').append(halfMoveClock)
        sb.append(' ').append(fullMoveNumber)
        if (variant == ChessVariant.THREE_CHECK) {
            sb.append(" +${3 - blackChecksRemaining}+${3 - whiteChecksRemaining}")
        }
        return sb.toString()
    }

    fun validateForEditor(): List<String> {
        val errors = mutableListOf<String>()
        var whiteKings = 0
        var blackKings = 0
        for (i in 0..63) {
            val p = board[i] ?: continue
            val rank = i shr 3
            if (p.type == PieceType.KING) {
                if (p.color == PieceColor.WHITE) whiteKings++ else blackKings++
            }
            if (p.type == PieceType.PAWN && (rank == 0 || rank == 7)) {
                if (variant != ChessVariant.HORDE || p.color == PieceColor.BLACK || rank == 7) {
                    errors.add("Pawn cannot remain on rank ${rank + 1} (${Square(i).algebraic}).")
                }
            }
        }
        if (variant != ChessVariant.ANTICHESS && variant != ChessVariant.HORDE && variant != ChessVariant.ATOMIC) {
            if (whiteKings != 1) errors.add("White must have exactly 1 King (found $whiteKings).")
            if (blackKings != 1) errors.add("Black must have exactly 1 King (found $blackKings).")
        } else if (variant == ChessVariant.HORDE && blackKings != 1) {
            errors.add("Black must have exactly 1 King in Horde.")
        }
        if (errors.isEmpty() && variant != ChessVariant.ANTICHESS && variant != ChessVariant.RACING_KINGS) {
            if (MoveGenerator.isKingInCheck(this, sideToMove.opposite)) {
                errors.add("Side not to move cannot already be in check.")
            }
        }
        return errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessBoardState) return false
        return board.contentEquals(other.board) &&
            sideToMove == other.sideToMove &&
            castlingRights == other.castlingRights &&
            enPassantSquare == other.enPassantSquare &&
            halfMoveClock == other.halfMoveClock &&
            fullMoveNumber == other.fullMoveNumber &&
            variant == other.variant &&
            whiteChecksRemaining == other.whiteChecksRemaining &&
            blackChecksRemaining == other.blackChecksRemaining &&
            whitePocket == other.whitePocket &&
            blackPocket == other.blackPocket
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + sideToMove.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantSquare?.hashCode() ?: 0)
        result = 31 * result + variant.hashCode()
        return result
    }

    companion object {
        const val STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        const val HORDE_FEN = "rnbqkbnr/pppppppp/8/1PP2PP1/PPPPPPPP/PPPPPPPP/PPPPPPPP/PPPPPPPP w kq - 0 1"
        const val RACING_KINGS_FEN = "8/8/8/8/8/8/krbnNBRK/qrbnNBRQ w - - 0 1"

        fun standard(): ChessBoardState = fromFen(STANDARD_FEN, ChessVariant.STANDARD).getOrThrow()

        fun initialForVariant(variant: ChessVariant, chess960Index: Int? = null): ChessBoardState =
            when (variant) {
                ChessVariant.CHESS960 -> chess960(chess960Index ?: Random.nextInt(960))
                ChessVariant.HORDE -> fromFen(HORDE_FEN, ChessVariant.HORDE).getOrThrow()
                ChessVariant.RACING_KINGS -> fromFen(RACING_KINGS_FEN, ChessVariant.RACING_KINGS).getOrThrow()
                else -> fromFen(STANDARD_FEN, variant).getOrThrow()
            }

        /**
         * Official Scharnagl numbering (0..959) for Chess960 / Fischer Random Chess.
         */
        fun chess960(index: Int): ChessBoardState {
            val id = index.coerceIn(0, 959)
            val backRank = arrayOfNulls<PieceType>(8)
            val n1 = id % 4
            val q1 = id / 4
            val b1 = q1 % 4
            val q2 = q1 / 4
            val qPos = q2 % 6
            val knightsId = q2 / 6

            // Light-square bishop (files 1, 3, 5, 7) and dark-square bishop (files 0, 2, 4, 6)
            backRank[n1 * 2 + 1] = PieceType.BISHOP
            backRank[b1 * 2] = PieceType.BISHOP

            // Queen on remaining empty square
            var emptySeen = 0
            for (f in 0..7) {
                if (backRank[f] == null) {
                    if (emptySeen == qPos) {
                        backRank[f] = PieceType.QUEEN
                        break
                    }
                    emptySeen++
                }
            }

            // Two knights in remaining 5 empty squares (10 combinations)
            val knightPairs = arrayOf(
                0 to 1, 0 to 2, 0 to 3, 0 to 4,
                1 to 2, 1 to 3, 1 to 4,
                2 to 3, 2 to 4,
                3 to 4,
            )
            val (ka, kb) = knightPairs[knightsId]
            emptySeen = 0
            for (f in 0..7) {
                if (backRank[f] == null) {
                    if (emptySeen == ka || emptySeen == kb) {
                        backRank[f] = PieceType.KNIGHT
                    }
                    emptySeen++
                }
            }

            // Remaining 3 squares are Rook, King, Rook in order
            val remaining = (0..7).filter { backRank[it] == null }
            val leftRook = remaining[0]
            val kingFile = remaining[1]
            val rightRook = remaining[2]
            backRank[leftRook] = PieceType.ROOK
            backRank[kingFile] = PieceType.KING
            backRank[rightRook] = PieceType.ROOK

            val board = arrayOfNulls<Piece>(64)
            for (f in 0..7) {
                val type = backRank[f]!!
                board[f] = Piece(type, PieceColor.WHITE)
                board[8 + f] = Piece(PieceType.PAWN, PieceColor.WHITE)
                board[48 + f] = Piece(PieceType.PAWN, PieceColor.BLACK)
                board[56 + f] = Piece(type, PieceColor.BLACK)
            }
            return ChessBoardState(
                board = board,
                sideToMove = PieceColor.WHITE,
                castlingRights = CastlingRights(
                    whiteKingSideFile = rightRook,
                    whiteQueenSideFile = leftRook,
                    blackKingSideFile = rightRook,
                    blackQueenSideFile = leftRook,
                ),
                variant = ChessVariant.CHESS960,
            )
        }

        fun fromFen(fen: String, variant: ChessVariant = ChessVariant.STANDARD): Result<ChessBoardState> = runCatching {
            val trimmed = fen.trim()
            require(trimmed.isNotEmpty()) { "FEN cannot be empty." }
            val parts = trimmed.split(Regex("\\s+"))
            require(parts.size >= 4) { "FEN must contain at least 4 fields (board, turn, castling, en passant)." }

            val boardPartRaw = parts[0]
            var boardPart = boardPartRaw
            val whitePocket = mutableMapOf<PieceType, Int>()
            val blackPocket = mutableMapOf<PieceType, Int>()
            if ('[' in boardPartRaw && boardPartRaw.endsWith(']')) {
                val idx = boardPartRaw.indexOf('[')
                boardPart = boardPartRaw.substring(0, idx)
                val pocketStr = boardPartRaw.substring(idx + 1, boardPartRaw.length - 1)
                for (ch in pocketStr) {
                    val p = Piece.fromFenChar(ch) ?: error("Invalid pocket piece '$ch' in FEN.")
                    val map = if (p.color == PieceColor.WHITE) whitePocket else blackPocket
                    map[p.type] = (map[p.type] ?: 0) + 1
                }
            }

            val ranks = boardPart.split('/')
            require(ranks.size == 8) { "FEN board must have exactly 8 ranks (found ${ranks.size})." }

            val board = arrayOfNulls<Piece>(64)
            val promoted = mutableSetOf<Int>()
            for (rIdx in 0..7) {
                val rank = 7 - rIdx
                val rankStr = ranks[rIdx]
                var file = 0
                var i = 0
                while (i < rankStr.length) {
                    val ch = rankStr[i]
                    if (ch in '1'..'8') {
                        file += (ch - '0')
                    } else if (ch == '~' && file > 0) {
                        promoted.add((rank shl 3) or (file - 1))
                    } else {
                        val piece = Piece.fromFenChar(ch) ?: error("Invalid FEN piece character '$ch'.")
                        require(file in 0..7) { "Rank ${rank + 1} exceeds 8 files." }
                        board[(rank shl 3) or file] = piece
                        file++
                    }
                    i++
                }
                require(file == 8) { "Rank ${rank + 1} has $file files instead of 8." }
            }

            val side = when (parts[1]) {
                "w" -> PieceColor.WHITE
                "b" -> PieceColor.BLACK
                else -> error("Invalid active color '${parts[1]}' in FEN.")
            }

            // Locate kings and rooks for castling rights
            val whiteKingFile = (0..7).firstOrNull { board[it] == Piece(PieceType.KING, PieceColor.WHITE) }
            val blackKingFile = (0..7).firstOrNull { board[56 + it] == Piece(PieceType.KING, PieceColor.BLACK) }

            var wk: Int? = null
            var wq: Int? = null
            var bk: Int? = null
            var bq: Int? = null
            val castlingStr = parts[2]
            if (castlingStr != "-") {
                for (c in castlingStr) {
                    when (c) {
                        'K' -> wk = (7 downTo 0).firstOrNull { f -> board[f] == Piece(PieceType.ROOK, PieceColor.WHITE) && (whiteKingFile == null || f > whiteKingFile) } ?: 7
                        'Q' -> wq = (0..7).firstOrNull { f -> board[f] == Piece(PieceType.ROOK, PieceColor.WHITE) && (whiteKingFile == null || f < whiteKingFile) } ?: 0
                        'k' -> bk = (7 downTo 0).firstOrNull { f -> board[56 + f] == Piece(PieceType.ROOK, PieceColor.BLACK) && (blackKingFile == null || f > blackKingFile) } ?: 7
                        'q' -> bq = (0..7).firstOrNull { f -> board[56 + f] == Piece(PieceType.ROOK, PieceColor.BLACK) && (blackKingFile == null || f < blackKingFile) } ?: 0
                        in 'A'..'H' -> {
                            val f = c - 'A'
                            if (whiteKingFile != null && f > whiteKingFile) wk = f else wq = f
                        }
                        in 'a'..'h' -> {
                            val f = c - 'a'
                            if (blackKingFile != null && f > blackKingFile) bk = f else bq = f
                        }
                        else -> error("Invalid castling character '$c' in FEN.")
                    }
                }
            }

            val epSquare = if (parts[3] == "-") null else {
                Square.fromAlgebraic(parts[3]) ?: error("Invalid en passant square '${parts[3]}'.")
            }

            val halfMove = parts.getOrNull(4)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val fullMove = parts.getOrNull(5)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            var whiteRem = 3
            var blackRem = 3
            val checksPart = parts.getOrNull(6)
            if (checksPart != null && checksPart.startsWith("+")) {
                val nums = checksPart.drop(1).split('+').mapNotNull { it.toIntOrNull() }
                if (nums.size == 2) {
                    blackRem = (3 - nums[0]).coerceIn(0, 3)
                    whiteRem = (3 - nums[1]).coerceIn(0, 3)
                }
            }

            ChessBoardState(
                board = board,
                sideToMove = side,
                castlingRights = CastlingRights(wk, wq, bk, bq),
                enPassantSquare = epSquare,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove,
                variant = variant,
                whiteChecksRemaining = whiteRem,
                blackChecksRemaining = blackRem,
                whitePocket = whitePocket,
                blackPocket = blackPocket,
                promotedSquares = promoted,
            )
        }
    }
}
