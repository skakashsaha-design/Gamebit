package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardArrow
import com.example.chess.BoardHighlight
import com.example.chess.ChessBoardState
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.ui.theme.BoardThemeColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardView(
    state: ChessBoardState,
    legalMoves: List<Move>,
    theme: BoardThemeColors,
    modifier: Modifier = Modifier,
    flipped: Boolean = false,
    interactive: Boolean = true,
    allowBothSides: Boolean = true,
    humanSide: PieceColor = PieceColor.WHITE,
    showCoordinates: Boolean = true,
    showLegalMoves: Boolean = true,
    showLastMove: Boolean = true,
    autoQueen: Boolean = false,
    pieceStyleId: String = "classic",
    moveAnimation: Boolean = true,
    reducedAnimations: Boolean = false,
    animationSpeed: Float = 1.0f,
    annotationMode: Boolean = false,
    lastMove: Move? = null,
    premoveQueue: List<Move> = emptyList(),
    arrows: List<BoardArrow> = emptyList(),
    highlights: List<BoardHighlight> = emptyList(),
    selectedDropPiece: PieceType? = null,
    onMoveSelected: (Move) -> Unit = {},
    onPremoveQueued: (Move) -> Unit = {},
    onSquareTappedRaw: ((Square) -> Unit)? = null,
    onToggleCircleSquare: ((Square) -> Unit)? = null,
    onToggleUserArrow: ((Square, Square) -> Unit)? = null,
) {
    var selectedSquare by remember(state.positionKey(), flipped) { mutableStateOf<Square?>(null) }
    var dragOrigin by remember { mutableStateOf<Square?>(null) }
    var dragPointerOffset by remember { mutableStateOf<Offset?>(null) }
    var pendingPromotionMove by remember { mutableStateOf<Pair<Square, Square>?>(null) }

    val animProgress = remember { Animatable(1f) }
    LaunchedEffect(lastMove, state.positionKey(), moveAnimation, reducedAnimations) {
        if (lastMove != null && moveAnimation && !reducedAnimations) {
            animProgress.snapTo(0f)
            val durMs = (160f / animationSpeed.coerceIn(0.5f, 2.0f)).toInt().coerceIn(60, 320)
            animProgress.animateTo(1f, tween(durationMillis = durMs))
        } else {
            animProgress.snapTo(1f)
        }
    }

    val inCheckSquare = remember(state) {
        if (MoveGenerator.isKingInCheck(state, state.sideToMove)) {
            MoveGenerator.findKingSquare(state, state.sideToMove)
        } else null
    }

    val activeLegalTargets = remember(selectedSquare, legalMoves) {
        val sel = selectedSquare ?: return@remember emptySet()
        legalMoves.filter { it.dropPieceType == null && it.from == sel }.map { it.to.index }.toSet()
    }

    fun tryExecuteOrQueue(from: Square, to: Square) {
        if (from == to) {
            selectedSquare = null
            return
        }
        val canMoveNow = allowBothSides || state.sideToMove == humanSide
        if (canMoveNow) {
            val matches = legalMoves.filter { it.dropPieceType == null && it.from == from && it.to == to }
            if (matches.isNotEmpty()) {
                val promoMatches = matches.filter { it.promotion != null }
                if (promoMatches.isNotEmpty()) {
                    if (autoQueen) {
                        val queenMove = promoMatches.firstOrNull { it.promotion == PieceType.QUEEN } ?: promoMatches.first()
                        selectedSquare = null
                        onMoveSelected(queenMove)
                    } else {
                        pendingPromotionMove = from to to
                    }
                } else {
                    selectedSquare = null
                    onMoveSelected(matches.first())
                }
                return
            }
        } else {
            val piece = state.pieceAt(from)
            if (piece != null && piece.color == humanSide) {
                val isPromo = piece.type == PieceType.PAWN && (to.rank == 7 || to.rank == 0)
                onPremoveQueued(Move(from, to, promotion = if (isPromo) PieceType.QUEEN else null))
                selectedSquare = null
                return
            }
        }
        val tappedPiece = state.pieceAt(to)
        if (tappedPiece != null && (allowBothSides || tappedPiece.color == humanSide)) {
            selectedSquare = to
        } else {
            selectedSquare = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("chess_board")
                .semantics {
                    contentDescription = "Chessboard, ${if (state.sideToMove == PieceColor.WHITE) "White" else "Black"} to move"
                },
        ) {
            val boardPx = constraints.maxWidth.toFloat()
            val sqSize = boardPx / 8f

            fun offsetToSquare(offset: Offset): Square? {
                if (sqSize <= 0f) return null
                val col = (offset.x / sqSize).toInt().coerceIn(0, 7)
                val row = (offset.y / sqSize).toInt().coerceIn(0, 7)
                val file = if (flipped) 7 - col else col
                val rank = if (flipped) row else 7 - row
                return Square.of(file, rank)
            }

            fun squareTopLeft(sq: Square, cell: Float): Offset {
                val col = if (flipped) 7 - sq.file else sq.file
                val row = if (flipped) sq.rank else 7 - sq.rank
                return Offset(col * cell, row * cell)
            }

            fun squareCenter(sq: Square): Offset {
                val col = if (flipped) 7 - sq.file else sq.file
                val row = if (flipped) sq.rank else 7 - sq.rank
                return Offset((col + 0.5f) * sqSize, (row + 0.5f) * sqSize)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, legalMoves, flipped, interactive, selectedDropPiece, annotationMode) {
                        if (!interactive) return@pointerInput
                        detectTapGestures { offset ->
                            val tapped = offsetToSquare(offset) ?: return@detectTapGestures
                            if (annotationMode) {
                                onToggleCircleSquare?.invoke(tapped)
                                return@detectTapGestures
                            }
                            if (onSquareTappedRaw != null) {
                                onSquareTappedRaw(tapped)
                                return@detectTapGestures
                            }
                            if (selectedDropPiece != null) {
                                val dropMove = legalMoves.firstOrNull {
                                    it.dropPieceType == selectedDropPiece && it.to == tapped
                                }
                                if (dropMove != null) {
                                    onMoveSelected(dropMove)
                                    return@detectTapGestures
                                }
                            }
                            val currentSel = selectedSquare
                            if (currentSel == null) {
                                val p = state.pieceAt(tapped)
                                if (p != null && (allowBothSides || p.color == humanSide)) {
                                    selectedSquare = tapped
                                }
                            } else {
                                tryExecuteOrQueue(currentSel, tapped)
                            }
                        }
                    }
                    .pointerInput(state, legalMoves, flipped, interactive, annotationMode) {
                        if (!interactive || onSquareTappedRaw != null) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                val sq = offsetToSquare(offset) ?: return@detectDragGestures
                                if (annotationMode) {
                                    dragOrigin = sq
                                    dragPointerOffset = offset
                                    return@detectDragGestures
                                }
                                val p = state.pieceAt(sq)
                                if (p != null && (allowBothSides || p.color == humanSide)) {
                                    dragOrigin = sq
                                    selectedSquare = sq
                                    dragPointerOffset = offset
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragPointerOffset = change.position
                            },
                            onDragEnd = {
                                val from = dragOrigin
                                val endPos = dragPointerOffset
                                dragOrigin = null
                                dragPointerOffset = null
                                if (from != null && endPos != null) {
                                    val dest = offsetToSquare(endPos)
                                    if (dest != null && dest != from) {
                                        if (annotationMode) {
                                            onToggleUserArrow?.invoke(from, dest)
                                        } else {
                                            tryExecuteOrQueue(from, dest)
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                dragOrigin = null
                                dragPointerOffset = null
                            },
                        )
                    },
            ) {
                val cell = size.width / 8f
                val progress = animProgress.value
                val animLastMove = if (progress < 0.999f) lastMove else null

                for (row in 0..7) {
                    for (col in 0..7) {
                        val file = if (flipped) 7 - col else col
                        val rank = if (flipped) row else 7 - row
                        val sq = Square.of(file, rank)
                        val topLeft = Offset(col * cell, row * cell)
                        val baseColor = if (sq.isLight) theme.lightSquare else theme.darkSquare
                        drawRect(color = baseColor, topLeft = topLeft, size = Size(cell, cell))

                        if (showLastMove && lastMove != null && (lastMove.from == sq || lastMove.to == sq)) {
                            val hl = if (sq.isLight) theme.lastMoveLight else theme.lastMoveDark
                            drawRect(color = hl, topLeft = topLeft, size = Size(cell, cell))
                        }

                        if (premoveQueue.any { it.from == sq || it.to == sq }) {
                            drawRect(color = theme.premoveSquare, topLeft = topLeft, size = Size(cell, cell))
                        }

                        if (selectedSquare == sq) {
                            drawRect(color = theme.selectedSquare, topLeft = topLeft, size = Size(cell, cell))
                        }

                        if (inCheckSquare == sq) {
                            drawRect(color = theme.checkHighlight, topLeft = topLeft, size = Size(cell, cell))
                        }

                        highlights.firstOrNull { it.square == sq }?.let { customHl ->
                            drawCircle(
                                color = Color(customHl.colorHex),
                                radius = cell * 0.44f,
                                center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f),
                                style = Stroke(width = cell * 0.08f),
                            )
                        }

                        val piece = state.pieceAt(sq)
                        val isBeingDragged = !annotationMode && dragOrigin == sq && dragPointerOffset != null
                        val isAnimDest = animLastMove != null && animLastMove.to == sq
                        if (piece != null && !isBeingDragged && !isAnimDest) {
                            drawChessPiece(piece, topLeft, cell, pieceStyleId)
                        }

                        if (showLegalMoves && sq.index in activeLegalTargets) {
                            val center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
                            if (piece != null) {
                                drawCircle(
                                    color = Color(0x88000000),
                                    radius = cell * 0.44f,
                                    center = center,
                                    style = Stroke(width = cell * 0.09f),
                                )
                            } else {
                                drawCircle(
                                    color = Color(0x55000000),
                                    radius = cell * 0.16f,
                                    center = center,
                                )
                            }
                        }
                    }
                }

                // Draw smoothly sliding animated piece
                if (animLastMove != null) {
                    val destPiece = state.pieceAt(animLastMove.to)
                    if (destPiece != null) {
                        val startTl = squareTopLeft(animLastMove.from, cell)
                        val endTl = squareTopLeft(animLastMove.to, cell)
                        val curTl = Offset(
                            startTl.x + (endTl.x - startTl.x) * progress,
                            startTl.y + (endTl.y - startTl.y) * progress,
                        )
                        drawChessPiece(destPiece, curTl, cell, pieceStyleId)
                    }
                }

                // Draw arrows (engine best move, threats, user study annotations)
                arrows.forEach { arrow ->
                    drawBoardArrow(
                        start = squareCenter(arrow.from),
                        end = squareCenter(arrow.to),
                        color = Color(arrow.colorHex),
                        strokeWidth = cell * 0.14f,
                    )
                }

                // Draw floating piece under finger during drag
                val dOrigin = dragOrigin
                val dPos = dragPointerOffset
                if (!annotationMode && dOrigin != null && dPos != null) {
                    state.pieceAt(dOrigin)?.let { draggedPiece ->
                        val topLeft = Offset(dPos.x - cell * 0.55f, dPos.y - cell * 0.55f)
                        drawChessPiece(draggedPiece, topLeft, cell * 1.1f, pieceStyleId)
                    }
                }
            }

            if (showCoordinates) {
                val files = if (flipped) ('h' downTo 'a').toList() else ('a'..'h').toList()
                val ranks = if (flipped) (1..8).toList() else (8 downTo 1).toList()
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                ) {
                    files.forEachIndexed { col, ch ->
                        val isLight = if (flipped) col % 2 == 0 else col % 2 == 1
                        val textColor = if (isLight) theme.darkSquare else theme.lightSquare
                        Box(
                            modifier = Modifier.weight(1f).padding(end = 3.dp),
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Text(
                                text = ch.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 2.dp)
                        .fillMaxSize(),
                ) {
                    ranks.forEachIndexed { row, r ->
                        val isLight = row % 2 == 0
                        val textColor = if (isLight) theme.darkSquare else theme.lightSquare
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                            Text(
                                text = r.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                            )
                        }
                    }
                }
            }

            val promoPair = pendingPromotionMove
            if (promoPair != null) {
                val promoPieces = buildList {
                    add(PieceType.QUEEN)
                    add(PieceType.ROOK)
                    add(PieceType.BISHOP)
                    add(PieceType.KNIGHT)
                    if (state.variant == com.example.chess.ChessVariant.ANTICHESS) add(PieceType.KING)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .testTag("promotion_picker"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        promoPieces.forEach { pType ->
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable {
                                        val match = legalMoves.firstOrNull {
                                            it.from == promoPair.first &&
                                                it.to == promoPair.second &&
                                                it.promotion == pType
                                        }
                                        pendingPromotionMove = null
                                        selectedSquare = null
                                        if (match != null) onMoveSelected(match)
                                    }
                                    .testTag("promo_${pType.fenSymbol}"),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    drawChessPiece(Piece(pType, state.sideToMove), Offset.Zero, size.minDimension, pieceStyleId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceMiniIcon(
    piece: Piece,
    modifier: Modifier = Modifier,
    pieceStyleId: String = "classic",
) {
    Canvas(modifier = modifier) {
        drawChessPiece(piece, Offset.Zero, size.minDimension, pieceStyleId)
    }
}

private fun DrawScope.drawBoardArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val angle = atan2(dy, dx)
    val headLen = strokeWidth * 2.1f
    val lineEnd = Offset(
        end.x - cos(angle) * headLen * 0.55f,
        end.y - sin(angle) * headLen * 0.55f,
    )
    drawLine(
        color = color,
        start = start,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    val p1 = Offset(
        end.x - headLen * cos(angle - 0.55f),
        end.y - headLen * sin(angle - 0.55f),
    )
    val p2 = Offset(
        end.x - headLen * cos(angle + 0.55f),
        end.y - headLen * sin(angle + 0.55f),
    )
    val head = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(head, color = color)
}

/**
 * Multi-style geometric Staunton chess piece renderer supporting 6 distinct visual themes:
 * classic, modern, minimal, wooden, high_contrast, and 3d.
 */
fun DrawScope.drawChessPiece(
    piece: Piece,
    topLeft: Offset,
    cellSize: Float,
    pieceStyleId: String = "classic",
) {
    val isWhite = piece.color == PieceColor.WHITE
    val (fill, stroke, detail) = when (pieceStyleId) {
        "modern" -> if (isWhite) Triple(Color(0xFFEAF2FF), Color(0xFF1E3A5F), Color(0xFF3B82F6))
        else Triple(Color(0xFF1E293B), Color(0xFFCBD5E1), Color(0xFF60A5FA))
        "minimal" -> if (isWhite) Triple(Color(0xFFFFFFFF), Color(0xFF334155), Color(0xFF64748B))
        else Triple(Color(0xFF27303F), Color(0xFFF1F5F9), Color(0xFF94A3B8))
        "wooden" -> if (isWhite) Triple(Color(0xFFF6DFB2), Color(0xFF4A2511), Color(0xFF8B5A2B))
        else Triple(Color(0xFF5C2E14), Color(0xFFF3D5A5), Color(0xFFA06B3E))
        "high_contrast" -> if (isWhite) Triple(Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF000000))
        else Triple(Color(0xFF050505), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
        "3d" -> if (isWhite) Triple(Color(0xFFFFFCF5), Color(0xFF0F172A), Color(0xFFD97706))
        else Triple(Color(0xFF171E29), Color(0xFFE2E8F0), Color(0xFF38BDF8))
        else -> if (isWhite) Triple(Color(0xFFFDFBF7), Color(0xFF1E293B), Color(0xFF334155))
        else Triple(Color(0xFF1A1F26), Color(0xFFE2E8F0), Color(0xFF94A3B8))
    }

    val swMult = when (pieceStyleId) {
        "high_contrast", "minimal" -> 0.062f
        else -> 0.045f
    }
    val sw = (cellSize * swMult).coerceAtLeast(1.4f)
    val shadowOffset = if (pieceStyleId == "3d") cellSize * 0.055f else 0f

    fun drawPass(tl: Offset, passFill: Color, passStroke: Color, passDetail: Color, strokeOnly: Boolean = false) {
        fun pt(nx: Float, ny: Float) = Offset(tl.x + nx * cellSize, tl.y + ny * cellSize)
        when (piece.type) {
            PieceType.PAWN -> {
                val body = Path().apply {
                    moveTo(pt(0.28f, 0.84f).x, pt(0.28f, 0.84f).y)
                    lineTo(pt(0.72f, 0.84f).x, pt(0.72f, 0.84f).y)
                    lineTo(pt(0.65f, 0.72f).x, pt(0.65f, 0.72f).y)
                    lineTo(pt(0.57f, 0.44f).x, pt(0.57f, 0.44f).y)
                    lineTo(pt(0.43f, 0.44f).x, pt(0.43f, 0.44f).y)
                    lineTo(pt(0.35f, 0.72f).x, pt(0.35f, 0.72f).y)
                    close()
                }
                drawPath(body, color = passFill)
                if (!strokeOnly) drawPath(body, color = passStroke, style = Stroke(width = sw))
                drawCircle(color = passFill, radius = cellSize * 0.145f, center = pt(0.5f, 0.30f))
                if (!strokeOnly) drawCircle(color = passStroke, radius = cellSize * 0.145f, center = pt(0.5f, 0.30f), style = Stroke(width = sw))
            }
            PieceType.ROOK -> {
                val rook = Path().apply {
                    moveTo(pt(0.24f, 0.84f).x, pt(0.24f, 0.84f).y)
                    lineTo(pt(0.76f, 0.84f).x, pt(0.76f, 0.84f).y)
                    lineTo(pt(0.71f, 0.72f).x, pt(0.71f, 0.72f).y)
                    lineTo(pt(0.66f, 0.36f).x, pt(0.66f, 0.36f).y)
                    lineTo(pt(0.74f, 0.31f).x, pt(0.74f, 0.31f).y)
                    lineTo(pt(0.74f, 0.18f).x, pt(0.74f, 0.18f).y)
                    lineTo(pt(0.62f, 0.18f).x, pt(0.62f, 0.18f).y)
                    lineTo(pt(0.62f, 0.25f).x, pt(0.62f, 0.25f).y)
                    lineTo(pt(0.55f, 0.25f).x, pt(0.55f, 0.25f).y)
                    lineTo(pt(0.55f, 0.18f).x, pt(0.55f, 0.18f).y)
                    lineTo(pt(0.45f, 0.18f).x, pt(0.45f, 0.18f).y)
                    lineTo(pt(0.45f, 0.25f).x, pt(0.45f, 0.25f).y)
                    lineTo(pt(0.38f, 0.25f).x, pt(0.38f, 0.25f).y)
                    lineTo(pt(0.38f, 0.18f).x, pt(0.38f, 0.18f).y)
                    lineTo(pt(0.26f, 0.18f).x, pt(0.26f, 0.18f).y)
                    lineTo(pt(0.26f, 0.31f).x, pt(0.26f, 0.31f).y)
                    lineTo(pt(0.34f, 0.36f).x, pt(0.34f, 0.36f).y)
                    lineTo(pt(0.29f, 0.72f).x, pt(0.29f, 0.72f).y)
                    close()
                }
                drawPath(rook, color = passFill)
                if (!strokeOnly) drawPath(rook, color = passStroke, style = Stroke(width = sw))
            }
            PieceType.KNIGHT -> {
                val knight = Path().apply {
                    moveTo(pt(0.25f, 0.84f).x, pt(0.25f, 0.84f).y)
                    lineTo(pt(0.75f, 0.84f).x, pt(0.75f, 0.84f).y)
                    lineTo(pt(0.71f, 0.56f).x, pt(0.71f, 0.56f).y)
                    lineTo(pt(0.64f, 0.28f).x, pt(0.64f, 0.28f).y)
                    lineTo(pt(0.54f, 0.16f).x, pt(0.54f, 0.16f).y)
                    lineTo(pt(0.47f, 0.23f).x, pt(0.47f, 0.23f).y)
                    lineTo(pt(0.35f, 0.24f).x, pt(0.35f, 0.24f).y)
                    lineTo(pt(0.22f, 0.43f).x, pt(0.22f, 0.43f).y)
                    lineTo(pt(0.30f, 0.50f).x, pt(0.30f, 0.50f).y)
                    lineTo(pt(0.46f, 0.45f).x, pt(0.46f, 0.45f).y)
                    lineTo(pt(0.48f, 0.52f).x, pt(0.48f, 0.52f).y)
                    lineTo(pt(0.31f, 0.70f).x, pt(0.31f, 0.70f).y)
                    close()
                }
                drawPath(knight, color = passFill)
                if (!strokeOnly) {
                    drawPath(knight, color = passStroke, style = Stroke(width = sw))
                    drawCircle(color = passDetail, radius = cellSize * 0.036f, center = pt(0.41f, 0.32f))
                }
            }
            PieceType.BISHOP -> {
                val base = Path().apply {
                    moveTo(pt(0.25f, 0.84f).x, pt(0.25f, 0.84f).y)
                    lineTo(pt(0.75f, 0.84f).x, pt(0.75f, 0.84f).y)
                    lineTo(pt(0.66f, 0.68f).x, pt(0.66f, 0.68f).y)
                    lineTo(pt(0.65f, 0.42f).x, pt(0.65f, 0.42f).y)
                    lineTo(pt(0.50f, 0.22f).x, pt(0.50f, 0.22f).y)
                    lineTo(pt(0.35f, 0.42f).x, pt(0.35f, 0.42f).y)
                    lineTo(pt(0.34f, 0.68f).x, pt(0.34f, 0.68f).y)
                    close()
                }
                drawPath(base, color = passFill)
                if (!strokeOnly) drawPath(base, color = passStroke, style = Stroke(width = sw))
                drawCircle(color = passFill, radius = cellSize * 0.06f, center = pt(0.50f, 0.16f))
                if (!strokeOnly) {
                    drawCircle(color = passStroke, radius = cellSize * 0.06f, center = pt(0.50f, 0.16f), style = Stroke(width = sw))
                    if (pieceStyleId != "minimal") {
                        drawLine(color = passDetail, start = pt(0.43f, 0.47f), end = pt(0.57f, 0.35f), strokeWidth = sw)
                    }
                }
            }
            PieceType.QUEEN -> {
                val queen = Path().apply {
                    moveTo(pt(0.23f, 0.84f).x, pt(0.23f, 0.84f).y)
                    lineTo(pt(0.77f, 0.84f).x, pt(0.77f, 0.84f).y)
                    lineTo(pt(0.82f, 0.32f).x, pt(0.82f, 0.32f).y)
                    lineTo(pt(0.66f, 0.50f).x, pt(0.66f, 0.50f).y)
                    lineTo(pt(0.61f, 0.24f).x, pt(0.61f, 0.24f).y)
                    lineTo(pt(0.50f, 0.46f).x, pt(0.50f, 0.46f).y)
                    lineTo(pt(0.39f, 0.24f).x, pt(0.39f, 0.24f).y)
                    lineTo(pt(0.34f, 0.50f).x, pt(0.34f, 0.50f).y)
                    lineTo(pt(0.18f, 0.32f).x, pt(0.18f, 0.32f).y)
                    close()
                }
                drawPath(queen, color = passFill)
                if (!strokeOnly) {
                    drawPath(queen, color = passStroke, style = Stroke(width = sw))
                    listOf(pt(0.18f, 0.28f), pt(0.39f, 0.20f), pt(0.61f, 0.20f), pt(0.82f, 0.28f)).forEach { c ->
                        drawCircle(color = passFill, radius = cellSize * 0.045f, center = c)
                        drawCircle(color = passStroke, radius = cellSize * 0.045f, center = c, style = Stroke(width = sw * 0.8f))
                    }
                }
            }
            PieceType.KING -> {
                val king = Path().apply {
                    moveTo(pt(0.24f, 0.84f).x, pt(0.24f, 0.84f).y)
                    lineTo(pt(0.76f, 0.84f).x, pt(0.76f, 0.84f).y)
                    lineTo(pt(0.78f, 0.44f).x, pt(0.78f, 0.44f).y)
                    lineTo(pt(0.61f, 0.33f).x, pt(0.61f, 0.33f).y)
                    lineTo(pt(0.50f, 0.42f).x, pt(0.50f, 0.42f).y)
                    lineTo(pt(0.39f, 0.33f).x, pt(0.39f, 0.33f).y)
                    lineTo(pt(0.22f, 0.44f).x, pt(0.22f, 0.44f).y)
                    close()
                }
                drawPath(king, color = passFill)
                if (!strokeOnly) {
                    drawPath(king, color = passStroke, style = Stroke(width = sw))
                    drawLine(color = passStroke, start = pt(0.50f, 0.12f), end = pt(0.50f, 0.32f), strokeWidth = sw * 1.4f, cap = StrokeCap.Round)
                    drawLine(color = passStroke, start = pt(0.41f, 0.20f), end = pt(0.59f, 0.20f), strokeWidth = sw * 1.4f, cap = StrokeCap.Round)
                }
            }
        }
    }

    if (shadowOffset > 0f) {
        drawPass(
            tl = Offset(topLeft.x + shadowOffset, topLeft.y + shadowOffset),
            passFill = Color(0x55000000),
            passStroke = Color.Transparent,
            passDetail = Color.Transparent,
            strokeOnly = true,
        )
    }
    drawPass(topLeft, fill, stroke, detail, strokeOnly = false)
}

