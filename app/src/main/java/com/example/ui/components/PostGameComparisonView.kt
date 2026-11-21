package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.GameReviewReport
import com.example.chess.GameStatus
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.ReviewedMove
import com.example.ui.theme.EvalBest
import com.example.ui.theme.EvalBlunder
import com.example.ui.theme.EvalBrilliant
import com.example.ui.theme.EvalGreat
import com.example.ui.theme.EvalInaccuracy
import com.example.ui.theme.EvalMiss
import com.example.ui.theme.EvalMistake
import com.example.viewmodel.RatingDeltaResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostGameComparisonDialog(
    gameStatus: GameStatus,
    winner: PieceColor?,
    whiteName: String,
    blackName: String,
    eloDelta: RatingDeltaResult?,
    report: GameReviewReport?,
    reviewedMoves: List<ReviewedMove>,
    isGeneratingSummary: Boolean,
    reducedAnimations: Boolean,
    savedToArchive: Boolean,
    onReviewGame: () -> Unit,
    onAnalyze: () -> Unit,
    onRematch: () -> Unit,
    onNewGame: () -> Unit,
    onShareGame: () -> Unit,
    onSaveGame: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPly by remember(reviewedMoves.size) {
        mutableIntStateOf(reviewedMoves.lastOrNull()?.ply ?: 1)
    }

    val headline = when (winner) {
        PieceColor.WHITE -> "$whiteName Wins!"
        PieceColor.BLACK -> "$blackName Wins!"
        null -> "Draw"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .testTag("post_game_summary_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = gameStatus.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (eloDelta != null && eloDelta.ratedGame) {
                        val deltaPrefix = if (eloDelta.delta >= 0) "+${eloDelta.delta}" else "${eloDelta.delta}"
                        val deltaColor = if (eloDelta.delta >= 0) EvalBest else EvalBlunder
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = deltaColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, deltaColor.copy(alpha = 0.6f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = "${eloDelta.newRating} ($deltaPrefix)",
                                    fontWeight = FontWeight.Bold,
                                    color = deltaColor,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = eloDelta.category.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (reviewedMoves.isNotEmpty()) {
                    EvaluationGraph(
                        reviewedMoves = reviewedMoves,
                        selectedPly = selectedPly,
                        onSelectPly = { selectedPly = it },
                        reducedAnimations = reducedAnimations,
                    )
                } else if (isGeneratingSummary) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                            Text("Computing accuracy, phase breakdown & turning points...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (report != null) {
                    // Player vs Opponent Accuracy & Phase Table
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ComparisonRow(
                                label = "Player",
                                whiteVal = whiteName,
                                blackVal = blackName,
                                bold = true,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ComparisonRow(
                                label = "Accuracy",
                                whiteVal = "${report.whiteAccuracy}%",
                                blackVal = "${report.blackAccuracy}%",
                                bold = true,
                                highlightColor = EvalBest,
                            )
                            ComparisonRow(
                                label = "Opening",
                                whiteVal = report.whiteOpeningAccuracy?.let { "$it%" } ?: "--",
                                blackVal = report.blackOpeningAccuracy?.let { "$it%" } ?: "--",
                            )
                            ComparisonRow(
                                label = "Middlegame",
                                whiteVal = report.whiteMiddlegameAccuracy?.let { "$it%" } ?: "--",
                                blackVal = report.blackMiddlegameAccuracy?.let { "$it%" } ?: "--",
                            )
                            ComparisonRow(
                                label = "Endgame",
                                whiteVal = report.whiteEndgameAccuracy?.let { "$it%" } ?: "--",
                                blackVal = report.blackEndgameAccuracy?.let { "$it%" } ?: "--",
                            )
                            ComparisonRow(
                                label = "Avg CP Loss",
                                whiteVal = "${report.whiteAcpl}",
                                blackVal = "${report.blackAcpl}",
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            val tiers = listOf(
                                Triple("Brilliant", MoveQuality.BRILLIANT, EvalBrilliant),
                                Triple("Great", MoveQuality.GREAT, EvalGreat),
                                Triple("Best", MoveQuality.BEST, EvalBest),
                                Triple("Inaccuracy", MoveQuality.INACCURACY, EvalInaccuracy),
                                Triple("Mistake", MoveQuality.MISTAKE, EvalMistake),
                                Triple("Miss", MoveQuality.MISS, EvalMiss),
                                Triple("Blunder", MoveQuality.BLUNDER, EvalBlunder),
                            )
                            tiers.forEach { (title, q, c) ->
                                val wCount = report.whiteQualityCounts[q] ?: 0
                                val bCount = report.blackQualityCounts[q] ?: 0
                                if (wCount > 0 || bCount > 0 || q == MoveQuality.BEST || q == MoveQuality.MISTAKE || q == MoveQuality.BLUNDER) {
                                    ComparisonRow(
                                        label = "${q.symbol} $title",
                                        whiteVal = wCount.toString(),
                                        blackVal = bCount.toString(),
                                        labelColor = c,
                                    )
                                }
                            }
                        }
                    }

                    // Key Turning Points / Where White or Black Stood Best
                    if (report.turningPoints.isNotEmpty() || report.advantageSummary.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("Game Story & Turning Points", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                if (report.advantageSummary.isNotBlank()) {
                                    Text(report.advantageSummary, style = MaterialTheme.typography.bodySmall)
                                }
                                report.turningPoints.take(3).forEach { tp ->
                                    Text("• ${tp.title}: ${tp.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // 6 Action Buttons (Review Game, Analyze, Rematch, New Game, Share Game, Save Game)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onReviewGame,
                        modifier = Modifier.weight(1f, fill = false).testTag("review_game_btn"),
                    ) { Text("Review Game") }
                    OutlinedButton(
                        onClick = onAnalyze,
                        modifier = Modifier.weight(1f, fill = false).testTag("postgame_analyze_btn"),
                    ) { Text("Analyze") }
                    Button(
                        onClick = onRematch,
                        modifier = Modifier.weight(1f, fill = false).testTag("rematch_btn"),
                    ) { Text("Rematch") }
                    OutlinedButton(
                        onClick = onNewGame,
                        modifier = Modifier.weight(1f, fill = false).testTag("postgame_new_game_btn"),
                    ) { Text("New Game") }
                    OutlinedButton(
                        onClick = onShareGame,
                        modifier = Modifier.weight(1f, fill = false).testTag("result_pgn_btn"),
                    ) { Text("Share Game") }
                    OutlinedButton(
                        onClick = onSaveGame,
                        modifier = Modifier.weight(1f, fill = false).testTag("postgame_save_btn"),
                    ) { Text(if (savedToArchive) "Saved ✓" else "Save Game") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    whiteVal: String,
    blackVal: String,
    bold: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = whiteVal,
            modifier = Modifier.weight(0.28f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            modifier = Modifier.weight(0.44f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = blackVal,
            modifier = Modifier.weight(0.28f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
