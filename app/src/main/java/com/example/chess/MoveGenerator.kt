package com.example.chess

import kotlin.math.abs

object MoveGenerator {
    private val KNIGHT_DELTAS = arrayOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    private val KING_DELTAS = arrayOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    private val BISHOP_DIRS = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val ROOK_DIRS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val HILL_SQUARES = setOf(
        Square.of(3, 3).index, Square.of(4, 3).index,
        Square.of(3, 4).index, Square.of(4, 4).index,
    )

    fun legalMoves(state: ChessBoardState): List<Move> {
        val status = evaluateStatusWithoutHistory(state)
        if (status.isTerminal) return emptyList()

        val pseudo = generatePseudoLegalMoves(state)
        val legal = ArrayList<Move>(pseudo.size)

        for (move in pseudo) {
            val next = applyMoveUnchecked(state, move)
            when (state.variant) {
                ChessVariant.ANTICHESS -> {
                    // No check constraints in Antichess
                    legal.add(move)
                }
                ChessVariant.ATOMIC -> {
                    // Own king cannot be exploded; if own king still exists, it cannot be left in check
                    // (unless opponent king was exploded or adjacent kings rule applies)
                    val ownKing = findKingSquare(next, state.sideToMove)
                    if (ownKing != null) {
                        val oppKing = findKingSquare(next, state.sideToMove.opposite)
                        if (oppKing == null) {
                            // Opponent king exploded -> immediate win, legal!
                            legal.add(move)
                        } else if (!kingsAdjacent(ownKing, oppKing) && !isSquareAttacked(next, ownKing, state.sideToMove.opposite)) {
                            legal.add(move)
                        }
                    }
                }
                ChessVariant.RACING_KINGS -> {
                    // Neither king may ever be placed under attack after any move in Racing Kings
                    val ownKing = findKingSquare(next, state.sideToMove)
                    val oppKing = findKingSquare(next, state.sideToMove.opposite)
                    val ownAttacked = ownKing != null && isSquareAttacked(next, ownKing, state.sideToMove.opposite)
                    val oppAttacked = oppKing != null && isSquareAttacked(next, oppKing, state.sideToMove)
                    if (!ownAttacked && !oppAttacked) {
                        legal.add(move)
                    }
                }
                else -> {
                    if (!isKingInCheck(next, state.sideToMove)) {
                        legal.add(move)
                    }
                }
            }
        }

        if (state.variant == ChessVariant.ANTICHESS) {
            val captures = legal.filter { move ->
                move.dropPieceType == null && (state.pieceAt(move.to) != null || move.isEnPassant)
            }
            if (captures.isNotEmpty()) return captures
        }

        return legal
    }

    private fun kingsAdjacent(a: Square, b: Square): Boolean =
        abs(a.file - b.file) <= 1 && abs(a.rank - b.rank) <= 1

    private fun generatePseudoLegalMoves(state: ChessBoardState): List<Move> {
        val moves = ArrayList<Move>(48)
        val side = state.sideToMove
        val forward = if (side == PieceColor.WHITE) 1 else -1
        val startRank = if (side == PieceColor.WHITE) 1 else 6
        val promoRank = if (side == PieceColor.WHITE) 7 else 0

        for (i in 0..63) {
            val piece = state.board[i] ?: continue
            if (piece.color != side) continue
            val from = Square(i)
            val f = from.file
            val r = from.rank

            when (piece.type) {
                PieceType.PAWN -> {
                    val nr = r + forward
                    if (nr in 0..7) {
                        val oneStep = Square.of(f, nr)
                        if (state.pieceAt(oneStep) == null) {
                            addPawnMoves(moves, from, oneStep, nr == promoRank, state.variant)
                            val canDouble = r == startRank || (state.variant == ChessVariant.HORDE && side == PieceColor.WHITE && r == 0)
                            val nnr = r + 2 * forward
                            if (canDouble && nnr in 0..7) {
                                val twoStep = Square.of(f, nnr)
                                if (state.pieceAt(twoStep) == null) {
                                    moves.add(Move(from, twoStep))
                                }
                            }
                        }
                        for (df in intArrayOf(-1, 1)) {
                            val nf = f + df
                            if (nf in 0..7) {
                                val target = Square.of(nf, nr)
                                val victim = state.pieceAt(target)
                                if (victim != null && victim.color != side) {
                                    addPawnMoves(moves, from, target, nr == promoRank, state.variant)
                                } else if (state.enPassantSquare == target) {
                                    moves.add(Move(from, target, isEnPassant = true))
                                }
                            }
                        }
                    }
                }
                PieceType.KNIGHT -> {
                    for ((df, dr) in KNIGHT_DELTAS) {
                        val nf = f + df
                        val nr = r + dr
                        if (nf in 0..7 && nr in 0..7) {
                            val target = Square.of(nf, nr)
                            val occ = state.pieceAt(target)
                            if (occ == null || occ.color != side) moves.add(Move(from, target))
                        }
                    }
                }
                PieceType.BISHOP -> addSlidingMoves(state, moves, from, side, BISHOP_DIRS)
                PieceType.ROOK -> addSlidingMoves(state, moves, from, side, ROOK_DIRS)
                PieceType.QUEEN -> {
                    addSlidingMoves(state, moves, from, side, BISHOP_DIRS)
                    addSlidingMoves(state, moves, from, side, ROOK_DIRS)
                }
                PieceType.KING -> {
                    for ((df, dr) in KING_DELTAS) {
                        val nf = f + df
                        val nr = r + dr
                        if (nf in 0..7 && nr in 0..7) {
                            val target = Square.of(nf, nr)
                            val occ = state.pieceAt(target)
                            if (occ == null) {
                                moves.add(Move(from, target))
                            } else if (occ.color != side && state.variant != ChessVariant.ATOMIC) {
                                moves.add(Move(from, target))
                            }
                        }
                    }
                    if (state.variant != ChessVariant.ANTICHESS && state.variant != ChessVariant.RACING_KINGS) {
                        addCastlingMoves(state, moves, from, side)
                    }
                }
            }
        }

        if (state.variant == ChessVariant.CRAZYHOUSE) {
            val pocket = if (side == PieceColor.WHITE) state.whitePocket else state.blackPocket
            for ((pieceType, count) in pocket) {
                if (count <= 0) continue
                for (i in 0..63) {
                    if (state.board[i] != null) continue
                    val rank = i shr 3
                    if (pieceType == PieceType.PAWN && (rank == 0 || rank == 7)) continue
                    val sq = Square(i)
                    moves.add(Move(sq, sq, dropPieceType = pieceType))
                }
            }
        }

        return moves
    }

    private fun addPawnMoves(
        moves: MutableList<Move>,
        from: Square,
        to: Square,
        isPromotion: Boolean,
        variant: ChessVariant,
    ) {
        if (!isPromotion) {
            moves.add(Move(from, to))
            return
        }
        moves.add(Move(from, to, promotion = PieceType.QUEEN))
        moves.add(Move(from, to, promotion = PieceType.ROOK))
        moves.add(Move(from, to, promotion = PieceType.BISHOP))
        moves.add(Move(from, to, promotion = PieceType.KNIGHT))
        if (variant == ChessVariant.ANTICHESS) {
            moves.add(Move(from, to, promotion = PieceType.KING))
        }
    }

    private fun addSlidingMoves(
        state: ChessBoardState,
        moves: MutableList<Move>,
        from: Square,
        side: PieceColor,
        dirs: Array<Pair<Int, Int>>,
    ) {
        for ((df, dr) in dirs) {
            var nf = from.file + df
            var nr = from.rank + dr
            while (nf in 0..7 && nr in 0..7) {
                val target = Square.of(nf, nr)
                val occ = state.pieceAt(target)
                if (occ == null) {
                    moves.add(Move(from, target))
                } else {
                    if (occ.color != side) moves.add(Move(from, target))
                    break
                }
                nf += df
                nr += dr
            }
        }
    }

    private fun addCastlingMoves(
        state: ChessBoardState,
        moves: MutableList<Move>,
        kingSq: Square,
        side: PieceColor,
    ) {
        val homeRank = if (side == PieceColor.WHITE) 0 else 7
        if (kingSq.rank != homeRank) return
        if (isKingInCheck(state, side)) return

        val kRookFile = if (side == PieceColor.WHITE) state.castlingRights.whiteKingSideFile else state.castlingRights.blackKingSideFile
        val qRookFile = if (side == PieceColor.WHITE) state.castlingRights.whiteQueenSideFile else state.castlingRights.blackQueenSideFile

        if (kRookFile != null) {
            tryAddCastle(state, moves, kingSq, kRookFile, homeRank, targetKingFile = 6, targetRookFile = 5, side)
        }
        if (qRookFile != null) {
            tryAddCastle(state, moves, kingSq, qRookFile, homeRank, targetKingFile = 2, targetRookFile = 3, side)
        }
    }

    private fun tryAddCastle(
        state: ChessBoardState,
        moves: MutableList<Move>,
        kingSq: Square,
        rookFile: Int,
        homeRank: Int,
        targetKingFile: Int,
        targetRookFile: Int,
        side: PieceColor,
    ) {
        val rookSq = Square.of(rookFile, homeRank)
        val rookPiece = state.pieceAt(rookSq)
        if (rookPiece != Piece(PieceType.ROOK, side)) return

        val minPath = minOf(kingSq.file, targetKingFile, rookFile, targetRookFile)
        val maxPath = maxOf(kingSq.file, targetKingFile, rookFile, targetRookFile)
        for (f in minPath..maxPath) {
            if (f == kingSq.file || f == rookFile) continue
            if (state.pieceAt(f, homeRank) != null) return
        }

        val step = if (targetKingFile >= kingSq.file) 1 else -1
        var f = kingSq.file
        while (true) {
            if (isSquareAttacked(state, Square.of(f, homeRank), side.opposite)) return
            if (f == targetKingFile) break
            f += step
        }

        // Standard chess encodes castling target as g1/c1; Chess960 internally also works seamlessly with target square
        val dest = if (state.variant == ChessVariant.CHESS960) rookSq else Square.of(targetKingFile, homeRank)
        moves.add(Move(kingSq, dest, isCastling = true))
    }

    fun applyMove(state: ChessBoardState, move: Move): ChessBoardState {
        val legal = legalMoves(state)
        require(legal.any { it.from == move.from && it.to == move.to && it.promotion == move.promotion && it.dropPieceType == move.dropPieceType }) {
            "Illegal move ${move.uci} for position ${state.toFen()}"
        }
        val matched = legal.first { it.from == move.from && it.to == move.to && it.promotion == move.promotion && it.dropPieceType == move.dropPieceType }
        return applyMoveUnchecked(state, matched)
    }

    fun applyMoveUnchecked(state: ChessBoardState, move: Move): ChessBoardState {
        val nextBoard = state.board.copyOf()
        val side = state.sideToMove
        val whitePocket = state.whitePocket.toMutableMap()
        val blackPocket = state.blackPocket.toMutableMap()
        val promoted = state.promotedSquares.toMutableSet()

        var newEp: Square? = null
        var rights = state.castlingRights
        var halfMove = state.halfMoveClock + 1

        if (move.dropPieceType != null) {
            val pocket = if (side == PieceColor.WHITE) whitePocket else blackPocket
            val currentCount = pocket[move.dropPieceType] ?: 0
            if (currentCount <= 1) pocket.remove(move.dropPieceType) else pocket[move.dropPieceType] = currentCount - 1
            nextBoard[move.to.index] = Piece(move.dropPieceType, side)
            halfMove = 0
        } else {
            val moving = nextBoard[move.from.index] ?: return state
            val captured = nextBoard[move.to.index]
            val wasPromoted = move.from.index in promoted
            promoted.remove(move.from.index)

            if (move.isCastling) {
                val homeRank = if (side == PieceColor.WHITE) 0 else 7
                val rookFile = if (state.variant == ChessVariant.CHESS960) move.to.file else if (move.to.file == 6) state.castlingRights.let { if (side == PieceColor.WHITE) it.whiteKingSideFile else it.blackKingSideFile } ?: 7 else state.castlingRights.let { if (side == PieceColor.WHITE) it.whiteQueenSideFile else it.blackQueenSideFile } ?: 0
                val isKingSide = rookFile > move.from.file
                val destKingSq = Square.of(if (isKingSide) 6 else 2, homeRank)
                val destRookSq = Square.of(if (isKingSide) 5 else 3, homeRank)
                nextBoard[move.from.index] = null
                nextBoard[Square.of(rookFile, homeRank).index] = null
                nextBoard[destKingSq.index] = moving
                nextBoard[destRookSq.index] = Piece(PieceType.ROOK, side)
                rights = clearSideCastling(rights, side)
            } else {
                nextBoard[move.from.index] = null
                var epCapturedSq: Square? = null
                if (move.isEnPassant) {
                    val capRank = if (side == PieceColor.WHITE) move.to.rank - 1 else move.to.rank + 1
                    epCapturedSq = Square.of(move.to.file, capRank)
                }

                val isCapture = captured != null || move.isEnPassant
                if (moving.type == PieceType.PAWN || isCapture) halfMove = 0

                if (state.variant == ChessVariant.CRAZYHOUSE && isCapture) {
                    val capPiece = captured ?: Piece(PieceType.PAWN, side.opposite)
                    val capIndex = epCapturedSq?.index ?: move.to.index
                    val pocketType = if (capIndex in promoted) PieceType.PAWN else capPiece.type
                    if (pocketType != PieceType.KING) {
                        val pocket = if (side == PieceColor.WHITE) whitePocket else blackPocket
                        pocket[pocketType] = (pocket[pocketType] ?: 0) + 1
                    }
                    promoted.remove(capIndex)
                }

                if (epCapturedSq != null) {
                    nextBoard[epCapturedSq.index] = null
                }

                if (state.variant == ChessVariant.ATOMIC && isCapture) {
                    // Capture explodes capturer and all non-pawn pieces in 3x3 radius
                    nextBoard[move.to.index] = null
                    for (df in -1..1) {
                        for (dr in -1..1) {
                            if (df == 0 && dr == 0) continue
                            val nf = move.to.file + df
                            val nr = move.to.rank + dr
                            if (nf in 0..7 && nr in 0..7) {
                                val adj = Square.of(nf, nr)
                                val adjPiece = nextBoard[adj.index]
                                if (adjPiece != null && adjPiece.type != PieceType.PAWN) {
                                    nextBoard[adj.index] = null
                                }
                            }
                        }
                    }
                } else {
                    val placed = if (move.promotion != null) Piece(move.promotion, side) else moving
                    nextBoard[move.to.index] = placed
                    if (move.promotion != null || wasPromoted) {
                        promoted.add(move.to.index)
                    }
                }

                if (moving.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
                    val epRank = (move.from.rank + move.to.rank) / 2
                    newEp = Square.of(move.from.file, epRank)
                }

                rights = updateCastlingRights(rights, moving, move.from, move.to)
            }
        }

        val provisionalNext = state.copy(
            board = nextBoard,
            sideToMove = side.opposite,
            castlingRights = rights,
            enPassantSquare = newEp,
            halfMoveClock = halfMove,
            fullMoveNumber = if (side == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber,
            whitePocket = whitePocket,
            blackPocket = blackPocket,
            promotedSquares = promoted,
        )

        return if (state.variant == ChessVariant.THREE_CHECK && isKingInCheck(provisionalNext, side.opposite)) {
            if (side == PieceColor.WHITE) {
                provisionalNext.copy(blackChecksRemaining = (state.blackChecksRemaining - 1).coerceAtLeast(0))
            } else {
                provisionalNext.copy(whiteChecksRemaining = (state.whiteChecksRemaining - 1).coerceAtLeast(0))
            }
        } else {
            provisionalNext
        }
    }

    private fun clearSideCastling(rights: CastlingRights, side: PieceColor): CastlingRights =
        if (side == PieceColor.WHITE) rights.copy(whiteKingSideFile = null, whiteQueenSideFile = null)
        else rights.copy(blackKingSideFile = null, blackQueenSideFile = null)

    private fun updateCastlingRights(rights: CastlingRights, moving: Piece, from: Square, to: Square): CastlingRights {
        var r = rights
        if (moving.type == PieceType.KING) {
            r = clearSideCastling(r, moving.color)
        }
        if (from.rank == 0) {
            if (from.file == r.whiteKingSideFile) r = r.copy(whiteKingSideFile = null)
            if (from.file == r.whiteQueenSideFile) r = r.copy(whiteQueenSideFile = null)
        }
        if (from.rank == 7) {
            if (from.file == r.blackKingSideFile) r = r.copy(blackKingSideFile = null)
            if (from.file == r.blackQueenSideFile) r = r.copy(blackQueenSideFile = null)
        }
        if (to.rank == 0) {
            if (to.file == r.whiteKingSideFile) r = r.copy(whiteKingSideFile = null)
            if (to.file == r.whiteQueenSideFile) r = r.copy(whiteQueenSideFile = null)
        }
        if (to.rank == 7) {
            if (to.file == r.blackKingSideFile) r = r.copy(blackKingSideFile = null)
            if (to.file == r.blackQueenSideFile) r = r.copy(blackQueenSideFile = null)
        }
        return r
    }

    fun findKingSquare(state: ChessBoardState, color: PieceColor): Square? {
        for (i in 0..63) {
            val p = state.board[i]
            if (p != null && p.type == PieceType.KING && p.color == color) return Square(i)
        }
        return null
    }

    fun hasLegalEnPassantCapture(state: ChessBoardState): Boolean {
        val ep = state.enPassantSquare ?: return false
        val side = state.sideToMove
        val pawnRank = if (side == PieceColor.WHITE) ep.rank - 1 else ep.rank + 1
        if (pawnRank !in 0..7) return false
        for (df in intArrayOf(-1, 1)) {
            val f = ep.file + df
            if (f in 0..7 && state.pieceAt(f, pawnRank) == Piece(PieceType.PAWN, side)) {
                val epMove = Move(Square.of(f, pawnRank), ep, isEnPassant = true)
                val next = applyMoveUnchecked(state, epMove)
                val valid = when (state.variant) {
                    ChessVariant.ANTICHESS -> true
                    ChessVariant.ATOMIC -> {
                        val ownKing = findKingSquare(next, side)
                        val oppKing = findKingSquare(next, side.opposite)
                        ownKing != null && (oppKing == null || (!kingsAdjacent(ownKing, oppKing) && !isSquareAttacked(next, ownKing, side.opposite)))
                    }
                    ChessVariant.RACING_KINGS -> false
                    else -> !isKingInCheck(next, side)
                }
                if (valid) return true
            }
        }
        return false
    }

    fun isKingInCheck(state: ChessBoardState, color: PieceColor): Boolean {
        if (state.variant == ChessVariant.ANTICHESS || state.variant == ChessVariant.RACING_KINGS) return false
        val kingSq = findKingSquare(state, color) ?: return false
        if (state.variant == ChessVariant.ATOMIC) {
            val oppKing = findKingSquare(state, color.opposite) ?: return false
            if (kingsAdjacent(kingSq, oppKing)) return false
        }
        return isSquareAttacked(state, kingSq, color.opposite)
    }

    fun isSquareAttacked(state: ChessBoardState, target: Square, byColor: PieceColor): Boolean {
        val f = target.file
        val r = target.rank

        // Pawn attacks
        val pawnAttackRank = if (byColor == PieceColor.WHITE) r - 1 else r + 1
        if (pawnAttackRank in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val nf = f + df
                if (nf in 0..7 && state.pieceAt(nf, pawnAttackRank) == Piece(PieceType.PAWN, byColor)) return true
            }
        }

        // Knight attacks
        for ((df, dr) in KNIGHT_DELTAS) {
            val nf = f + df
            val nr = r + dr
            if (nf in 0..7 && nr in 0..7 && state.pieceAt(nf, nr) == Piece(PieceType.KNIGHT, byColor)) return true
        }

        // King attacks
        for ((df, dr) in KING_DELTAS) {
            val nf = f + df
            val nr = r + dr
            if (nf in 0..7 && nr in 0..7 && state.pieceAt(nf, nr) == Piece(PieceType.KING, byColor)) return true
        }

        // Diagonal sliders
        for ((df, dr) in BISHOP_DIRS) {
            var nf = f + df
            var nr = r + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = state.pieceAt(nf, nr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }

        // Orthogonal sliders
        for ((df, dr) in ROOK_DIRS) {
            var nf = f + df
            var nr = r + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = state.pieceAt(nf, nr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }

        return false
    }

    private fun evaluateStatusWithoutHistory(state: ChessBoardState): GameStatus {
        when (state.variant) {
            ChessVariant.THREE_CHECK -> {
                if (state.whiteChecksRemaining <= 0 || state.blackChecksRemaining <= 0) return GameStatus.VARIANT_WIN
            }
            ChessVariant.KING_OF_THE_HILL -> {
                val wk = findKingSquare(state, PieceColor.WHITE)
                val bk = findKingSquare(state, PieceColor.BLACK)
                if ((wk != null && wk.index in HILL_SQUARES) || (bk != null && bk.index in HILL_SQUARES)) {
                    return GameStatus.VARIANT_WIN
                }
            }
            ChessVariant.ATOMIC -> {
                if (findKingSquare(state, PieceColor.WHITE) == null || findKingSquare(state, PieceColor.BLACK) == null) {
                    return GameStatus.VARIANT_WIN
                }
            }
            ChessVariant.HORDE -> {
                val whitePieces = state.board.count { it?.color == PieceColor.WHITE }
                if (whitePieces == 0) return GameStatus.VARIANT_WIN
            }
            ChessVariant.ANTICHESS -> {
                val whitePieces = state.board.count { it?.color == PieceColor.WHITE }
                val blackPieces = state.board.count { it?.color == PieceColor.BLACK }
                if (whitePieces == 0 || blackPieces == 0) return GameStatus.VARIANT_WIN
            }
            ChessVariant.RACING_KINGS -> {
                val wk = findKingSquare(state, PieceColor.WHITE)
                val bk = findKingSquare(state, PieceColor.BLACK)
                if (bk != null && bk.rank == 7) return GameStatus.VARIANT_WIN
                if (wk != null && wk.rank == 7 && state.sideToMove == PieceColor.WHITE) return GameStatus.VARIANT_WIN
            }
            else -> Unit
        }
        return GameStatus.IN_PROGRESS
    }

    fun evaluateGameResult(
        state: ChessBoardState,
        positionCounts: Map<String, Int> = emptyMap(),
    ): Pair<GameStatus, PieceColor?> {
        val variantPreCheck = evaluateStatusWithoutHistory(state)
        if (variantPreCheck == GameStatus.VARIANT_WIN) {
            val winner = when (state.variant) {
                ChessVariant.THREE_CHECK -> if (state.blackChecksRemaining <= 0) PieceColor.WHITE else PieceColor.BLACK
                ChessVariant.KING_OF_THE_HILL -> {
                    val wk = findKingSquare(state, PieceColor.WHITE)
                    if (wk != null && wk.index in HILL_SQUARES) PieceColor.WHITE else PieceColor.BLACK
                }
                ChessVariant.ATOMIC -> if (findKingSquare(state, PieceColor.BLACK) == null) PieceColor.WHITE else PieceColor.BLACK
                ChessVariant.HORDE -> PieceColor.BLACK
                ChessVariant.ANTICHESS -> if (state.board.none { it?.color == PieceColor.WHITE }) PieceColor.WHITE else PieceColor.BLACK
                ChessVariant.RACING_KINGS -> {
                    val wk = findKingSquare(state, PieceColor.WHITE)
                    val bk = findKingSquare(state, PieceColor.BLACK)
                    when {
                        wk?.rank == 7 && bk?.rank == 7 -> null // Draw
                        wk?.rank == 7 -> PieceColor.WHITE
                        else -> PieceColor.BLACK
                    }
                }
                else -> null
            }
            return GameStatus.VARIANT_WIN to winner
        }

        val moves = legalMoves(state)
        if (moves.isEmpty()) {
            return when (state.variant) {
                ChessVariant.ANTICHESS -> GameStatus.VARIANT_WIN to state.sideToMove
                ChessVariant.HORDE -> {
                    if (state.sideToMove == PieceColor.BLACK && isKingInCheck(state, PieceColor.BLACK)) {
                        GameStatus.CHECKMATE to PieceColor.WHITE
                    } else {
                        GameStatus.STALEMATE to null
                    }
                }
                ChessVariant.RACING_KINGS -> {
                    val wk = findKingSquare(state, PieceColor.WHITE)
                    if (wk != null && wk.rank == 7 && state.sideToMove == PieceColor.BLACK) {
                        GameStatus.VARIANT_WIN to PieceColor.WHITE
                    } else {
                        GameStatus.STALEMATE to null
                    }
                }
                else -> {
                    if (isKingInCheck(state, state.sideToMove)) {
                        GameStatus.CHECKMATE to state.sideToMove.opposite
                    } else {
                        GameStatus.STALEMATE to null
                    }
                }
            }
        }

        val reps = positionCounts[state.positionKey()] ?: 0
        if (reps >= 5) return GameStatus.FIVEFOLD_REPETITION to null
        if (state.halfMoveClock >= 150) return GameStatus.SEVENTY_FIVE_MOVE_RULE to null
        if (reps >= 3) return GameStatus.THREEFOLD_REPETITION to null
        if (state.halfMoveClock >= 100) return GameStatus.FIFTY_MOVE_RULE to null

        if (state.variant in listOf(
                ChessVariant.STANDARD,
                ChessVariant.CHESS960,
                ChessVariant.THREE_CHECK,
                ChessVariant.ATOMIC,
            )
        ) {
            if (isInsufficientMaterial(state)) return GameStatus.INSUFFICIENT_MATERIAL to null
        }

        return GameStatus.IN_PROGRESS to null
    }

    fun isInsufficientMaterial(state: ChessBoardState): Boolean {
        val whiteMinors = mutableListOf<Pair<PieceType, Boolean>>()
        val blackMinors = mutableListOf<Pair<PieceType, Boolean>>()

        for (i in 0..63) {
            val p = state.board[i] ?: continue
            when (p.type) {
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                PieceType.KNIGHT, PieceType.BISHOP -> {
                    val isLight = Square(i).isLight
                    val list = if (p.color == PieceColor.WHITE) whiteMinors else blackMinors
                    list.add(p.type to isLight)
                }
                PieceType.KING -> Unit
            }
        }

        val totalMinors = whiteMinors.size + blackMinors.size
        if (state.variant == ChessVariant.THREE_CHECK || state.variant == ChessVariant.ATOMIC) {
            // In Three-Check or Atomic, K vs K is dead; any knight/bishop can deliver 3 checks or force atomic play
            return totalMinors == 0
        }

        if (totalMinors <= 1) return true // K vs K, K+B vs K, K+N vs K

        // FIDE Article 5.2.2: No knights, and ALL bishops on board share the same square color -> dead position
        val allMinors = whiteMinors + blackMinors
        if (allMinors.all { it.first == PieceType.BISHOP }) {
            val firstColor = allMinors.first().second
            if (allMinors.all { it.second == firstColor }) return true
        }
        return false
    }

    fun toSan(state: ChessBoardState, move: Move, legalMoves: List<Move> = legalMoves(state)): String {
        val sb = StringBuilder()
        if (move.dropPieceType != null) {
            if (move.dropPieceType != PieceType.PAWN) sb.append(move.dropPieceType.sanLetter)
            sb.append('@').append(move.to.algebraic)
        } else {
            val piece = state.pieceAt(move.from) ?: return move.uci
            if (move.isCastling) {
                val isKingSide = if (state.variant == ChessVariant.CHESS960) move.to.file > move.from.file else move.to.file == 6
                sb.append(if (isKingSide) "O-O" else "O-O-O")
            } else {
                val isCapture = state.pieceAt(move.to) != null || move.isEnPassant
                if (piece.type == PieceType.PAWN) {
                    if (isCapture) sb.append('a' + move.from.file).append('x')
                    sb.append(move.to.algebraic)
                    if (move.promotion != null) {
                        sb.append('=').append(move.promotion.sanLetter)
                    }
                } else {
                    sb.append(piece.type.sanLetter)
                    val ambiguous = legalMoves.filter {
                        it.dropPieceType == null &&
                            it.from != move.from &&
                            it.to == move.to &&
                            state.pieceAt(it.from)?.type == piece.type
                    }
                    if (ambiguous.isNotEmpty()) {
                        val sameFile = ambiguous.any { it.from.file == move.from.file }
                        val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                        if (!sameFile) sb.append('a' + move.from.file)
                        else if (!sameRank) sb.append('1' + move.from.rank)
                        else sb.append(move.from.algebraic)
                    }
                    if (isCapture) sb.append('x')
                    sb.append(move.to.algebraic)
                }
            }
        }

        val next = applyMoveUnchecked(state, move)
        if (state.variant != ChessVariant.ANTICHESS && state.variant != ChessVariant.RACING_KINGS) {
            if (isKingInCheck(next, next.sideToMove)) {
                val nextLegal = legalMoves(next)
                sb.append(if (nextLegal.isEmpty()) '#' else '+')
            }
        }
        return sb.toString()
    }

    fun parseSan(state: ChessBoardState, sanRaw: String): Move? {
        val legal = legalMoves(state)
        var san = sanRaw.trim().trimEnd('+', '#', '!', '?')
        if (san == "0-0") san = "O-O"
        if (san == "0-0-0") san = "O-O-O"

        if (san == "O-O" || san == "O-O-O") {
            return legal.firstOrNull { move ->
                move.isCastling && run {
                    val isKingSide = if (state.variant == ChessVariant.CHESS960) move.to.file > move.from.file else move.to.file == 6
                    (san == "O-O" && isKingSide) || (san == "O-O-O" && !isKingSide)
                }
            }
        }

        if ('@' in san) {
            val parts = san.split('@')
            if (parts.size != 2) return null
            val dropType = if (parts[0].isEmpty()) PieceType.PAWN else PieceType.fromChar(parts[0][0]) ?: return null
            val dest = Square.fromAlgebraic(parts[1]) ?: return null
            return legal.firstOrNull { it.dropPieceType == dropType && it.to == dest }
        }

        // Exact UCI fallback check
        if (san.length in 4..5) {
            legal.firstOrNull { it.uci == san.lowercase() }?.let { return it }
        }

        var promo: PieceType? = null
        val eqIdx = san.indexOf('=')
        if (eqIdx >= 0 && eqIdx + 1 < san.length) {
            promo = PieceType.fromChar(san[eqIdx + 1])
            san = san.substring(0, eqIdx)
        }

        val clean = san.replace("x", "")
        if (clean.length < 2) return null

        val destStr = clean.takeLast(2)
        val dest = Square.fromAlgebraic(destStr) ?: return null
        val prefix = clean.dropLast(2)

        val pieceType: PieceType
        val disambig: String
        if (prefix.isEmpty() || prefix[0].isLowerCase()) {
            pieceType = PieceType.PAWN
            disambig = prefix
        } else {
            pieceType = PieceType.fromChar(prefix[0]) ?: return null
            disambig = prefix.drop(1)
        }

        var reqFile: Int? = null
        var reqRank: Int? = null
        for (ch in disambig) {
            if (ch in 'a'..'h') reqFile = ch - 'a'
            else if (ch in '1'..'8') reqRank = ch - '1'
        }

        return legal.firstOrNull { move ->
            move.dropPieceType == null &&
                move.to == dest &&
                state.pieceAt(move.from)?.type == pieceType &&
                move.promotion == promo &&
                (reqFile == null || move.from.file == reqFile) &&
                (reqRank == null || move.from.rank == reqRank)
        }
    }
}
