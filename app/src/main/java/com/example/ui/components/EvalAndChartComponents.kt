package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.EvalInaccuracy
import com.example.ui.theme.EvalMistake

@Composable
fun EvaluationBar(
    topLine: EngineLine?,
    flipped: Boolean,
    modifier: Modifier = Modifier,
) {
    val whiteWinProb = (topLine?.whiteWinProbability ?: 0.5f).coerceIn(0.03f, 0.97f)
    val scoreText = topLine?.formattedScore ?: "0.00"

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
fun EvaluationGraph(
    reviewedMoves: List<ReviewedMove>,
    selectedPly: Int,
    onSelectPly: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reviewedMoves.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Evaluation Graph (Tap to jump)", style = MaterialTheme.typography.labelSmall)
            Text("White + / Black -", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .testTag("eval_graph")
                .pointerInput(reviewedMoves) {
                    detectTapGestures { offset ->
                        if (reviewedMoves.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * reviewedMoves.size).toInt()
                                .coerceIn(0, reviewedMoves.lastIndex)
                            onSelectPly(reviewedMoves[idx].ply)
                        }
                    }
                },
        ) {
            val midY = size.height / 2f
            drawLine(
                color = Color(0x5594A3B8),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5f,
            )

            val stepX = if (reviewedMoves.size <= 1) size.width else size.width / (reviewedMoves.size - 1)
            val linePath = Path()
            reviewedMoves.forEachIndexed { i, m ->
                val clamped = m.evalAfterCp.coerceIn(-800, 800)
                val x = i * stepX
                val y = midY - (clamped / 800f) * (size.height * 0.44f)
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, color = Color(0xFF10B981), style = Stroke(width = 3f))

            reviewedMoves.forEachIndexed { i, m ->
                val x = i * stepX
                val clamped = m.evalAfterCp.coerceIn(-800, 800)
                val y = midY - (clamped / 800f) * (size.height * 0.44f)
                if (m.ply == selectedPly) {
                    drawLine(
                        color = Color(0xAAFFFFFF),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2f,
                    )
                }
                val dotColor = when (m.quality) {
                    MoveQuality.BLUNDER -> EvalBlunder
                    MoveQuality.MISTAKE -> EvalMistake
                    MoveQuality.INACCURACY -> EvalInaccuracy
                    MoveQuality.BRILLIANT -> EvalBrilliant
                    MoveQuality.BEST -> EvalBest
                    else -> null
                }
                if (dotColor != null) {
                    drawCircle(color = dotColor, radius = 5.5f, center = Offset(x, y))
                }
            }
        }
    }
}
