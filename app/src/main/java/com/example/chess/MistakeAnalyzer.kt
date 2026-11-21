package com.example.chess

import kotlin.math.abs

data class MistakeAnalysisBreakdown(
    val movePlayedSan: String,
    val bestMoveSan: String,
    val evalBeforeFormatted: String,
    val evalAfterFormatted: String,
    val whatChanged: String,
    val opponentThreatCreated: String,
    val tacticalReason: String,
    val materialConsequence: String,
    val positionExplanation: String,
    val suggestedImprovement: String,
)

object MistakeAnalyzer {
    fun analyze(
        stateBefore: ChessBoardState,
        stateAfter: ChessBoardState,
        movePlayed: Move,
        playedSan: String,
        bestMoveSan: String,
        evalBeforeWhiteCp: Int,
        evalAfterWhiteCp: Int,
        cpLoss: Int,
        quality: MoveQuality,
    ): MistakeAnalysisBreakdown? {
        if (quality != MoveQuality.INACCURACY &&
            quality != MoveQuality.MISTAKE &&
            quality != MoveQuality.MISS &&
            quality != MoveQuality.BLUNDER
        ) {
            return null
        }

        val mover = stateBefore.sideToMove
        val opponent = mover.opposite
        val movedPiece = stateBefore.pieceAt(movePlayed.from)
        val moverIsWhite = mover == PieceColor.WHITE

        val evalBeforeStr = formatCp(evalBeforeWhiteCp)
        val evalAfterStr = formatCp(evalAfterWhiteCp)
        val pawnsLost = String.format(java.util.Locale.US, "%.2f", cpLoss / 100.0)

        // 1. Check hanging own pieces after move
        val undefendedTargets = mutableListOf<String>()
        for (i in 0..63) {
            val p = stateAfter.board[i] ?: continue
            if (p.color != mover || p.type == PieceType.KING || p.type == PieceType.PAWN) continue
            val sq = Square(i)
            val attacked = MoveGenerator.isSquareAttacked(stateAfter, sq, opponent)
            val defended = MoveGenerator.isSquareAttacked(stateAfter, sq, mover)
            if (attacked && !defended) {
                undefendedTargets.add("${p.type.name.lowercase().replaceFirstChar { it.uppercase() }} on ${sq.algebraic}")
            }
        }

        // 2. Check opponent checks / back rank threats after the move
        val opponentMoves = MoveGenerator.legalMoves(stateAfter)
        val checkingReply = opponentMoves.firstOrNull { reply ->
            val afterReply = MoveGenerator.applyMoveUnchecked(stateAfter, reply)
            MoveGenerator.isKingInCheck(afterReply, mover)
        }
        val mateInOneAllowed = opponentMoves.any { reply ->
            val afterReply = MoveGenerator.applyMoveUnchecked(stateAfter, reply)
            MoveGenerator.evaluateGameResult(afterReply).first == GameStatus.CHECKMATE
        }
        val captureReply = opponentMoves
            .mapNotNull { reply ->
                val victim = stateAfter.pieceAt(reply.to)
                if (victim != null && victim.color == mover) reply to victim else null
            }
            .maxByOrNull { it.second.type.materialValue }

        // 3. Evaluate tactical reason directly from board structure
        val moverKingSq = MoveGenerator.findKingSquare(stateAfter, mover)
        val backRankWeak = moverKingSq != null &&
            ((mover == PieceColor.WHITE && moverKingSq.rank == 0) || (mover == PieceColor.BLACK && moverKingSq.rank == 7)) &&
            MoveGenerator.legalMoves(stateAfter.copy(sideToMove = mover)).none { it.from == moverKingSq && it.to.rank != moverKingSq.rank }

        val tacticalReason = when {
            mateInOneAllowed -> "Allowed immediate mate threat on the very next turn"
            abs(evalBeforeWhiteCp) >= 45_000 -> "Missed a forced checkmate sequence ($bestMoveSan)"
            undefendedTargets.isNotEmpty() -> "Left piece undefended / hanging (${undefendedTargets.first()})"
            captureReply != null && captureReply.second.type.materialValue >= 300 ->
                "Allowed profitable capture against your ${captureReply.second.type.name.lowercase()} on ${captureReply.first.to.algebraic}"
            checkingReply != null && backRankWeak -> "King safety / back-rank vulnerability exposed to ${MoveGenerator.toSan(stateAfter, checkingReply, opponentMoves)}"
            checkingReply != null -> "Conceded forcing check (${MoveGenerator.toSan(stateAfter, checkingReply, opponentMoves)}) losing tempo"
            movedPiece?.type == PieceType.PAWN && (movePlayed.from.file in listOf(3, 4) || movePlayed.to.file in listOf(3, 4)) ->
                "Weakened central pawn structure and key outpost squares"
            else -> "Misplaced piece coordination or surrendered initiative (-$pawnsLost pawns)"
        }

        val threatCreated = when {
            mateInOneAllowed -> "Opponent has a forced checkmate threat immediately."
            captureReply != null ->
                "Opponent threatens ${MoveGenerator.toSan(stateAfter, captureReply.first, opponentMoves)}, winning material or forcing concessions."
            checkingReply != null ->
                "Opponent threatens ${MoveGenerator.toSan(stateAfter, checkingReply, opponentMoves)} with check and initiative."
            else -> "Opponent seizes the initiative and improves piece activity."
        }

        val matBefore = materialFor(stateBefore, mover)
        val matAfterBestEst = (cpLoss / 100.0)
        val materialConsequence = when {
            mateInOneAllowed -> "Decisive mate threat outweighs material."
            cpLoss >= 250 -> "Equivalent to losing ~${String.format(java.util.Locale.US, "%.1f", matAfterBestEst)} pawns of material/tactical value."
            cpLoss >= 90 -> "Costs roughly one pawn equivalent (-$pawnsLost)."
            else -> "Small positional concession (-$pawnsLost pawns); current material count is ${if (matBefore >= 0) "+$matBefore" else "$matBefore"} cp."
        }

        val shiftText = if ((moverIsWhite && evalBeforeWhiteCp > 50 && evalAfterWhiteCp < -50) ||
            (!moverIsWhite && evalBeforeWhiteCp < -50 && evalAfterWhiteCp > 50)
        ) {
            "Evaluation flipped from favorable ($evalBeforeStr) to disadvantaged ($evalAfterStr)."
        } else {
            "Evaluation shifted by -$pawnsLost pawns ($evalBeforeStr -> $evalAfterStr)."
        }

        return MistakeAnalysisBreakdown(
            movePlayedSan = playedSan,
            bestMoveSan = bestMoveSan,
            evalBeforeFormatted = evalBeforeStr,
            evalAfterFormatted = evalAfterStr,
            whatChanged = shiftText,
            opponentThreatCreated = threatCreated,
            tacticalReason = tacticalReason,
            materialConsequence = materialConsequence,
            positionExplanation = "$playedSan loosened key defenders or squares, giving the opponent concrete counterplay.",
            suggestedImprovement = "Play $bestMoveSan instead to keep your pieces protected and preserve your evaluation ($evalBeforeStr).",
        )
    }

    private fun formatCp(whiteCp: Int): String {
        if (abs(whiteCp) >= 45_000) return if (whiteCp > 0) "+M" else "-M"
        val pawns = whiteCp / 100.0
        return if (pawns >= 0) String.format(java.util.Locale.US, "+%.2f", pawns)
        else String.format(java.util.Locale.US, "%.2f", pawns)
    }

    private fun materialFor(state: ChessBoardState, color: PieceColor): Int {
        var diff = 0
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.type == PieceType.KING) continue
            diff += if (p.color == color) p.type.materialValue else -p.type.materialValue
        }
        return diff
    }
}
