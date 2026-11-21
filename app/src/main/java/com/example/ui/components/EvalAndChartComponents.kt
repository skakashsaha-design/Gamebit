package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.EngineLine
import com.example.chess.MoveQuality
import com.example.chess.ReviewedMove
import com.example.ui.theme.EvalBest
import com.example.ui.theme.EvalBlunder
import com.example.ui.theme.EvalBrilliant
import com.example.ui.theme.EvalGreat
import com.example.ui.theme.EvalInaccuracy
import com.example.ui.theme.EvalMiss
import com.example.ui.theme.EvalMistake
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tanh

@Composable
fun EvaluationBar(
    topLine: EngineLine?,
    flipped: Boolean,
    modifier: Modifier = Modifier,
) {
    val whiteWinProb = (topLine?.whiteWinProbability ?: 0.5f).coerceIn(0.03f, 0.97f)
    val scoreText = topLine?.let { formatGraphEval(it.scoreCp, it.mateIn) } ?: "0.0"

    Box(
        modifier = modifier
            .width(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .testTag("eval_bar"),
    ) {
        val topFraction = if (flipped) whiteWinProb else (1f - whiteWinProb)
        val topColor = if (flipped) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val bottomColor = if (flipped) Color(0xFF0F172A) else Color(0xFFF8FAFC)

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(topFraction.coerceAtLeast(0.03f))
                    .background(topColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight((1f - topFraction).coerceAtLeast(0.03f))
                    .background(bottomColor),
            )
        }

        val labelAtBottom = (topLine?.scoreCp ?: 0) >= 0 && !flipped
        Text(
            text = scoreText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (labelAtBottom) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            modifier = Modifier
                .align(if (labelAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(vertical = 3.dp),
        )
    }
}

@Composable
fun EvaluationPill(
    topLine: EngineLine?,
    modifier: Modifier = Modifier,
) {
    val cp = topLine?.scoreCp ?: 0
    val mate = topLine?.mateIn
    val label = formatGraphEval(cp, mate)
    val advantageColor = when {
        mate != null && mate > 0 -> EvalBest
        mate != null && mate < 0 -> EvalBlunder
        cp >= 80 -> EvalBest
        cp <= -80 -> EvalBlunder
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.testTag("evaluation_pill"),
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, advantageColor.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Evaluation:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = advantageColor,
            )
            if (topLine != null) {
                Text(
                    text = "d${topLine.depth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EvaluationGraph(
    reviewedMoves: List<ReviewedMove>,
    selectedPly: Int,
    onSelectPly: (Int) -> Unit,
    reducedAnimations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (reviewedMoves.isEmpty()) return
    val zoomState = remember { mutableIntStateOf(1) }
    val scrollState = rememberScrollState()
    val revealProgress = remember { Animatable(if (reducedAnimations) 1f else 0f) }

    LaunchedEffect(reviewedMoves.size, reducedAnimations) {
        if (reducedAnimations) {
            revealProgress.snapTo(1f)
        } else {
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            )
        }
    }

    val selectedMove = reviewedMoves.firstOrNull { it.ply == selectedPly } ?: reviewedMoves.last()
    val selectedEvalText = formatGraphEval(selectedMove.evalAfterCp, selectedMove.mateAfter)
    val selectedMoveNumber = (selectedMove.ply + 1) / 2
    val selectedMovePrefix = if (selectedMove.ply % 2 == 1) "$selectedMoveNumber." else "$selectedMoveNumber..."

    fun mapEvalToNormalized(rm: ReviewedMove): Float {
        // Maps White advantage to +1f (top) and Black advantage to -1f (bottom)
        val mate = rm.mateAfter
        if (mate != null) {
            return if (mate > 0) 0.96f else -0.96f
        }
        if (abs(rm.evalAfterCp) >= 45_000) {
            return if (rm.evalAfterCp > 0) 0.96f else -0.96f
        }
        return tanh(rm.evalAfterCp / 420.0).toFloat().coerceIn(-0.95f, 0.95f)
    }

    fun selectPlyFromX(xPx: Float, widthPx: Float) {
        if (widthPx <= 0f || reviewedMoves.isEmpty()) return
        val clampedRatio = (xPx / widthPx).coerceIn(0f, 1f)
        val idx = if (reviewedMoves.size == 1) 0
        else (clampedRatio * (reviewedMoves.size - 1)).roundToInt().coerceIn(0, reviewedMoves.lastIndex)
        onSelectPly(reviewedMoves[idx].ply)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E1620),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Evaluation Timeline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                    )
                    Text(
                        text = "$selectedMovePrefix ${selectedMove.san} • $selectedEvalText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = zoomState.intValue > 1,
                        onClick = { zoomState.intValue = if (zoomState.intValue >= 3) 1 else zoomState.intValue + 1 },
                        label = { Text("${zoomState.intValue}x", fontSize = 10.sp) },
                        modifier = Modifier.testTag("eval_graph_zoom"),
                    )
                    OutlinedButton(
                        onClick = { onSelectPly((selectedPly - 1).coerceAtLeast(1)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("eval_graph_prev"),
                    ) { Text("<", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { onSelectPly((selectedPly + 1).coerceAtMost(reviewedMoves.last().ply)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("eval_graph_next"),
                    ) { Text(">", fontSize = 11.sp) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Y-Axis Scale Labels
                Column(
                    modifier = Modifier.height(136.dp).padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text("+M", fontSize = 9.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                    Text("0.0", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    Text("-M", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }

                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                ) {
                    val baseWidth = if (maxWidth.value.isFinite() && maxWidth > 0.dp) maxWidth else 300.dp
                    val chartWidth = baseWidth * zoomState.intValue
                    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                        Canvas(
                            modifier = Modifier
                                .width(chartWidth)
                                .height(136.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141E2B))
                                .testTag("eval_graph")
                                .pointerInput(reviewedMoves, zoomState.intValue) {
                                    detectTapGestures { offset ->
                                        selectPlyFromX(offset.x, size.width.toFloat())
                                    }
                                }
                            .pointerInput(reviewedMoves, zoomState.intValue) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    selectPlyFromX(change.position.x, size.width.toFloat())
                                }
                            },
                    ) {
                        val w = size.width
                        val h = size.height
                        val padV = 10f
                        val usableHalf = (h - padV * 2f) / 2f
                        val midY = h / 2f

                        // Background Advantage Halves
                        drawRect(
                            color = Color(0x0DFFFFFF),
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(w, midY),
                        )
                        drawRect(
                            color = Color(0x18000000),
                            topLeft = Offset(0f, midY),
                            size = androidx.compose.ui.geometry.Size(w, midY),
                        )

                        // Reference guide lines (+2, 0.0, -2)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        val guideOffsets = listOf(0.45f, -0.45f)
                        guideOffsets.forEach { norm ->
                            val gy = midY - norm * usableHalf
                            drawLine(
                                color = Color(0x3394A3B8),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 1f,
                                pathEffect = dashEffect,
                            )
                        }
                        drawLine(
                            color = Color(0x88CBD5E1),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1.6f,
                        )

                        val pts = reviewedMoves.mapIndexed { i, rm ->
                            val x = if (reviewedMoves.size <= 1) w / 2f else (i.toFloat() / (reviewedMoves.size - 1)) * w
                            val y = midY - mapEvalToNormalized(rm) * usableHalf
                            Offset(x, y)
                        }

                        if (pts.isNotEmpty()) {
                            val linePath = Path()
                            val whiteFillPath = Path()
                            val blackFillPath = Path()

                            whiteFillPath.moveTo(pts.first().x, midY)
                            blackFillPath.moveTo(pts.first().x, midY)

                            pts.forEachIndexed { idx, p ->
                                if (idx == 0) {
                                    linePath.moveTo(p.x, p.y)
                                    whiteFillPath.lineTo(p.x, minOf(p.y, midY))
                                    blackFillPath.lineTo(p.x, maxOf(p.y, midY))
                                } else {
                                    val prev = pts[idx - 1]
                                    val ctrlX = (prev.x + p.x) / 2f
                                    linePath.cubicTo(ctrlX, prev.y, ctrlX, p.y, p.x, p.y)
                                    val prevWy = minOf(prev.y, midY)
                                    val curWy = minOf(p.y, midY)
                                    whiteFillPath.cubicTo(ctrlX, prevWy, ctrlX, curWy, p.x, curWy)
                                    val prevBy = maxOf(prev.y, midY)
                                    val curBy = maxOf(p.y, midY)
                                    blackFillPath.cubicTo(ctrlX, prevBy, ctrlX, curBy, p.x, curBy)
                                }
                            }
                            whiteFillPath.lineTo(pts.last().x, midY)
                            whiteFillPath.close()
                            blackFillPath.lineTo(pts.last().x, midY)
                            blackFillPath.close()

                            val clipW = w * revealProgress.value.coerceIn(0f, 1f)
                            clipRect(left = 0f, top = 0f, right = clipW, bottom = h) {
                                // White advantage area above center
                                drawPath(
                                    path = whiteFillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x99F8FAFC), Color(0x26E2E8F0)),
                                        startY = 0f,
                                        endY = midY,
                                    ),
                                )
                                // Black advantage area below center
                                drawPath(
                                    path = blackFillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x330F172A), Color(0xBB020617)),
                                        startY = midY,
                                        endY = h,
                                    ),
                                )
                                drawPath(
                                    path = linePath,
                                    color = Color(0xFF10B981),
                                    style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }

                            // Quality markers and active ply vertical cursor
                            reviewedMoves.forEachIndexed { idx, rm ->
                                val p = pts[idx]
                                if (p.x <= clipW + 2f) {
                                    val dotColor = when (rm.quality) {
                                        MoveQuality.BRILLIANT -> EvalBrilliant
                                        MoveQuality.GREAT -> EvalGreat
                                        MoveQuality.BEST -> EvalBest
                                        MoveQuality.INACCURACY -> EvalInaccuracy
                                        MoveQuality.MISTAKE -> EvalMistake
                                        MoveQuality.MISS -> EvalMiss
                                        MoveQuality.BLUNDER -> EvalBlunder
                                        else -> null
                                    }
                                    if (dotColor != null) {
                                        drawCircle(color = dotColor, radius = 4.8f, center = p)
                                    }
                                }
                                if (rm.ply == selectedPly) {
                                    drawLine(
                                        color = Color(0xFFF59E0B),
                                        start = Offset(p.x, 0f),
                                        end = Offset(p.x, h),
                                        strokeWidth = 2.6f,
                                    )
                                    drawCircle(color = Color(0xFFF59E0B), radius = 7.5f, center = p)
                                    drawCircle(color = Color(0xFF0F172A), radius = 3.5f, center = p)
                                }
                            }
                        }
                    }
                    }
                }
            }

            // Timeline move numbers row (spread cleanly across width)
            if (reviewedMoves.size >= 2) {
                val totalFullMoves = (reviewedMoves.last().ply + 1) / 2
                val milestones = listOf(
                    1,
                    (totalFullMoves / 4).coerceAtLeast(1),
                    (totalFullMoves / 2).coerceAtLeast(1),
                    ((totalFullMoves * 3) / 4).coerceAtLeast(1),
                    totalFullMoves,
                ).distinct()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    milestones.forEach { mv ->
                        Text(
                            text = "$mv",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                        )
                    }
                }
            }
        }
    }
}

fun formatGraphEval(cp: Int, mateIn: Int?): String = when {
    mateIn != null -> if (mateIn > 0) "M$mateIn" else "-M${abs(mateIn)}"
    abs(cp) >= 45_000 -> if (cp > 0) "M" else "-M"
    else -> {
        val pawns = cp / 100.0
        when {
            abs(pawns) < 0.05 -> "0.0"
            pawns > 0 -> String.format(Locale.US, "+%.1f", pawns)
            else -> String.format(Locale.US, "%.1f", pawns)
        }
    }
}
