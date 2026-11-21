package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.MoveQuality
import com.example.chess.ReviewedMove
import com.example.ui.theme.EvalBest
import com.example.ui.theme.EvalBlunder
import com.example.ui.theme.EvalBook
import com.example.ui.theme.EvalBrilliant
import com.example.ui.theme.EvalForced
import com.example.ui.theme.EvalGood
import com.example.ui.theme.EvalGreat
import com.example.ui.theme.EvalInaccuracy
import com.example.ui.theme.EvalMiss
import com.example.ui.theme.EvalMistake
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

fun colorForMoveQuality(quality: MoveQuality): Color = when (quality) {
    MoveQuality.BRILLIANT -> EvalBrilliant
    MoveQuality.GREAT -> EvalGreat
    MoveQuality.BEST -> EvalBest
    MoveQuality.EXCELLENT -> EvalBest.copy(alpha = 0.85f)
    MoveQuality.GOOD -> EvalGood
    MoveQuality.BOOK -> EvalBook
    MoveQuality.FORCED -> EvalForced
    MoveQuality.INACCURACY -> EvalInaccuracy
    MoveQuality.MISTAKE -> EvalMistake
    MoveQuality.MISS -> EvalMiss
    MoveQuality.BLUNDER -> EvalBlunder
}

@Composable
fun GamebitLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    showWordmark: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .semantics { contentDescription = "Gamebit logo emblem" },
            ) {
                val w = size.width
                val h = size.height
                // Crown crest
                val crown = Path().apply {
                    moveTo(w * 0.34f, h * 0.14f)
                    lineTo(w * 0.46f, h * 0.22f)
                    lineTo(w * 0.56f, h * 0.10f)
                    lineTo(w * 0.62f, h * 0.25f)
                    lineTo(w * 0.33f, h * 0.27f)
                    close()
                }
                drawPath(crown, color = Color(0xFF10B981))

                // Pedestal bar
                val pedestal = Path().apply {
                    moveTo(w * 0.15f, h * 0.86f)
                    lineTo(w * 0.85f, h * 0.86f)
                    lineTo(w * 0.88f, h * 0.96f)
                    lineTo(w * 0.12f, h * 0.96f)
                    close()
                }
                drawPath(pedestal, color = Color(0xFF10B981))

                // Knight silhouette
                val knight = Path().apply {
                    moveTo(w * 0.22f, h * 0.83f)
                    lineTo(w * 0.78f, h * 0.83f)
                    lineTo(w * 0.75f, h * 0.65f)
                    lineTo(w * 0.69f, h * 0.36f)
                    lineTo(w * 0.56f, h * 0.27f)
                    lineTo(w * 0.34f, h * 0.28f)
                    lineTo(w * 0.16f, h * 0.45f)
                    lineTo(w * 0.25f, h * 0.54f)
                    lineTo(w * 0.42f, h * 0.49f)
                    lineTo(w * 0.45f, h * 0.56f)
                    lineTo(w * 0.26f, h * 0.72f)
                    close()
                }
                drawPath(knight, color = Color(0xFFF8FAFC))

                drawCircle(
                    color = Color(0xFF10B981),
                    radius = w * 0.045f,
                    center = Offset(w * 0.34f, h * 0.37f),
                )
            }
        }
        if (showWordmark) {
            Column {
                Text(
                    text = "GAMEBIT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = "TACTICAL CHESS STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}

@Composable
fun GamebitSplashScreen(
    reducedAnimations: Boolean,
    onFinished: () -> Unit,
) {
    val scale = remember { Animatable(if (reducedAnimations) 1f else 0.82f) }
    val alpha = remember { Animatable(if (reducedAnimations) 1f else 0f) }

    LaunchedEffect(reducedAnimations) {
        if (reducedAnimations) {
            delay(180L)
        } else {
            alpha.animateTo(1f, animationSpec = tween(220))
            scale.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            delay(320L)
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090E14), Color(0xFF0F172A), Color(0xFF0B221D)),
                ),
            )
            .clickable { onFinished() }
            .testTag("gamebit_splash"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GamebitLogo(iconSize = 72.dp, showWordmark = false)
            Text(
                text = "GAMEBIT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF8FAFC),
                letterSpacing = 3.sp,
            )
            Text(
                text = "Play • Review • Master",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun GamebitDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun MoveQualityBadge(
    quality: MoveQuality,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bg = colorForMoveQuality(quality)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 6.dp else 99.dp),
        color = bg.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, bg.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 1.dp else 3.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(bg),
            )
            Text(
                text = if (compact) quality.symbol else "${quality.symbol} ${quality.label}",
                color = bg,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MoveExplanationDialog(
    reviewedMove: ReviewedMove,
    onDismiss: () -> Unit,
) {
    val moveNum = (reviewedMove.ply + 1) / 2
    val sidePrefix = if (reviewedMove.ply % 2 == 1) "$moveNum." else "$moveNum..."
    val beforeFmt = formatEvalScore(reviewedMove.evalBeforeCp, null)
    val afterFmt = formatEvalScore(reviewedMove.evalAfterCp, reviewedMove.mateAfter)

    GamebitDialog(
        onDismissRequest = onDismiss,
        title = "$sidePrefix ${reviewedMove.san}",
        subtitle = "Move Quality Breakdown",
        actions = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoveQualityBadge(quality = reviewedMove.quality)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "$beforeFmt → $afterFmt",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = reviewedMove.coachExplanation,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (reviewedMove.bestMoveSan.isNotBlank() && reviewedMove.bestMoveSan != reviewedMove.san) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Best Engine Choice", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EvalBest.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EvalBest.copy(alpha = 0.55f)),
                ) {
                    Text(
                        text = reviewedMove.bestMoveSan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = EvalBest,
                    )
                }
            }
        }

        reviewedMove.mistakeBreakdown?.let { mb ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Key Reason: ${mb.tacticalReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Text("Eval Impact: ${mb.whatChanged}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Threat: ${mb.opponentThreatCreated}", style = MaterialTheme.typography.bodySmall)
                Text("Tip: ${mb.suggestedImprovement}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatEvalScore(cp: Int, mateIn: Int?): String = when {
    mateIn != null -> if (mateIn > 0) "+M$mateIn" else "-M${abs(mateIn)}"
    abs(cp) >= 45_000 -> if (cp > 0) "+M" else "-M"
    else -> {
        val pawns = cp / 100.0
        if (pawns >= 0) String.format(Locale.US, "+%.1f", pawns)
        else String.format(Locale.US, "%.1f", pawns)
    }
}
