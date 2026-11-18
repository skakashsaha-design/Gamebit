package com.example.chess

data class EndgameClassification(
    val category: String,
    val detail: String,
    val isTheoreticalDraw: Boolean = false,
)

/**
 * Classifies board positions into endgame categories once material enters the endgame phase
 * (non-king piece count <= 10 or no queens and total minor/major pieces <= 4).
 */
object EndgameClassifier {
    fun classify(state: ChessBoardState): EndgameClassification? {
        var wp = 0; var bp = 0
        var wn = 0; var bn = 0
        var wb = 0; var bb = 0
        var wr = 0; var br = 0
        var wq = 0; var bq = 0
        var wDarkB = 0; var wLightB = 0
        var bDarkB = 0; var bLightB = 0

        for (i in 0..63) {
            val p = state.board[i] ?: continue
            val isLight = Square(i).isLight
            when (p.type) {
                PieceType.PAWN -> if (p.color == PieceColor.WHITE) wp++ else bp++
                PieceType.KNIGHT -> if (p.color == PieceColor.WHITE) wn++ else bn++
                PieceType.BISHOP -> {
                    if (p.color == PieceColor.WHITE) {
                        wb++
                        if (isLight) wLightB++ else wDarkB++
                    } else {
                        bb++
                        if (isLight) bLightB++ else bDarkB++
                    }
                }
                PieceType.ROOK -> if (p.color == PieceColor.WHITE) wr++ else br++
                PieceType.QUEEN -> if (p.color == PieceColor.WHITE) wq++ else bq++
                PieceType.KING -> Unit
            }
        }

        val totalQueens = wq + bq
        val totalRooks = wr + br
        val totalBishops = wb + bb
        val totalKnights = wn + bn
        val totalMinors = totalBishops + totalKnights
        val totalPawns = wp + bp
        val majorMinorCount = totalQueens * 2 + totalRooks + totalMinors

        val isEndgamePhase = (totalQueens == 0 && (totalRooks + totalMinors) <= 4) ||
            majorMinorCount <= 4 ||
            state.board.count { it != null } <= 10

        if (!isEndgamePhase) return null

        if (totalQueens == 0 && totalRooks == 0 && totalMinors == 0) {
            return if (totalPawns == 0) {
                EndgameClassification("Theoretical Draw", "Bare Kings (K vs K)", isTheoreticalDraw = true)
            } else {
                EndgameClassification("Pawn Endgame", "King & Pawn (${wp}P vs ${bp}P)")
            }
        }

        if (totalPawns == 0 && totalQueens == 0 && totalRooks == 0 && (totalBishops + totalKnights) == 1) {
            return EndgameClassification("Theoretical Draw", "Insufficient Mating Material", isTheoreticalDraw = true)
        }

        if (totalQueens > 0 && totalRooks > 0 && totalMinors == 0) {
            return EndgameClassification("Queen vs Rook Endgame", "Major pieces (${wq}Q+${wr}R vs ${bq}Q+${br}R)")
        }

        if (totalQueens > 0 && totalRooks == 0 && totalMinors == 0) {
            return EndgameClassification("Queen Endgame", "Queen & Pawn endgame (${wq}Q vs ${bq}Q)")
        }

        if (totalQueens == 0 && totalRooks > 0 && totalMinors == 0) {
            return EndgameClassification("Rook Endgame", "Rook & Pawn (${wr}R vs ${br}R)")
        }

        if (totalQueens == 0 && totalRooks > 0 && totalMinors > 0) {
            return EndgameClassification("Rook vs Minor Piece", "${wr}R/${wp}P vs ${br}R/${bn + bb}M")
        }

        if (totalQueens == 0 && totalRooks == 0 && totalBishops > 0 && totalKnights > 0) {
            return EndgameClassification("Bishop vs Knight", "Minor piece battle (${wb}B/${wn}N vs ${bb}B/${bn}N)")
        }

        if (totalQueens == 0 && totalRooks == 0 && totalBishops > 0 && totalKnights == 0) {
            val oppositeColored = wb == 1 && bb == 1 && ((wLightB == 1 && bDarkB == 1) || (wDarkB == 1 && bLightB == 1))
            val detail = if (oppositeColored) "Opposite-Colored Bishops (draw tendency)" else "Same-Colored Bishop Endgame"
            return EndgameClassification("Bishop Endgame", detail, isTheoreticalDraw = oppositeColored && kotlin.math.abs(wp - bp) <= 1)
        }

        if (totalQueens == 0 && totalRooks == 0 && totalKnights > 0 && totalBishops == 0) {
            return EndgameClassification("Knight Endgame", "Knight & Pawn (${wn}N vs ${bn}N)")
        }

        return EndgameClassification("Theoretical Endgame", "Reduced material technical endgame")
    }
}
