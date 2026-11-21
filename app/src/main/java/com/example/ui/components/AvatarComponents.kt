package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chess.BotProfile
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType

data class PlayerAvatarOption(
    val id: String,
    val label: String,
    val pieceType: PieceType,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val motif: String,
)

object PlayerAvatarCatalog {
    val ALL: List<PlayerAvatarOption> = listOf(
        PlayerAvatarOption("king", "Emerald King", PieceType.KING, 0xFF065F46, 0xFF022C22, 0xFF34D399, "crown"),
        PlayerAvatarOption("queen", "Crimson Queen", PieceType.QUEEN, 0xFF9F1239, 0xFF4C0519, 0xFFFB7185, "diamond"),
        PlayerAvatarOption("knight", "Cobalt Knight", PieceType.KNIGHT, 0xFF1E3A8A, 0xFF0F172A, 0xFF38BDF8, "helm"),
        PlayerAvatarOption("rook", "Obsidian Rook", PieceType.ROOK, 0xFF334155, 0xFF0F172A, 0xFF94A3B8, "shield"),
        PlayerAvatarOption("bishop", "Amethyst Bishop", PieceType.BISHOP, 0xFF5B21B6, 0xFF2E1065, 0xFFC084FC, "aurora"),
        PlayerAvatarOption("pawn", "Amber Pawn", PieceType.PAWN, 0xFFB45309, 0xFF451A03, 0xFFFBBF24, "sparks"),
        PlayerAvatarOption("grandmaster", "Grandmaster", PieceType.KING, 0xFF854D0E, 0xFF422006, 0xFFFACC15, "laurel"),
        PlayerAvatarOption("cyber", "Cyber Matrix", PieceType.KNIGHT, 0xFF0F172A, 0xFF020617, 0xFF06B6D4, "cyber"),
        PlayerAvatarOption("strategist", "Strategist", PieceType.BISHOP, 0xFF1D4ED8, 0xFF172554, 0xFF93C5FD, "glasses"),
        PlayerAvatarOption("blaze", "Blaze Gambit", PieceType.QUEEN, 0xFFC2410C, 0xFF431407, 0xFFFB923C, "flame"),
        PlayerAvatarOption("sentinel", "Iron Sentinel", PieceType.ROOK, 0xFF0F766E, 0xFF042F2E, 0xFF2DD4BF, "kabuto"),
        PlayerAvatarOption("aurora", "Aurora Star", PieceType.PAWN, 0xFF047857, 0xFF1E1B4B, 0xFF67E8F9, "sprout"),
    )

    fun forId(id: String): PlayerAvatarOption =
        ALL.firstOrNull { it.id == id } ?: ALL.first()
}

@Composable
fun BotAvatarView(
    bot: BotProfile,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    pieceStyleId: String = "classic",
    cornerRadius: Dp = 12.dp,
) {
    val spec = bot.avatarSpec
    val primary = Color(spec.primaryHex)
    val secondary = Color(spec.secondaryHex)
    val accent = Color(spec.accentHex)

    Surface(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${bot.name} bot avatar (${bot.styleTag})" },
        shape = RoundedCornerShape(cornerRadius),
        color = secondary,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAvatarBackdrop(primary = primary, secondary = secondary, accent = accent, motif = spec.emblemStyle)
                val pad = this.size.minDimension * 0.16f
                drawChessPiece(
                    piece = Piece(spec.badgePiece, PieceColor.WHITE),
                    topLeft = Offset(pad, pad),
                    cellSize = this.size.minDimension - pad * 2f,
                    pieceStyleId = pieceStyleId,
                )
                drawAvatarForegroundAccent(accent = accent, motif = spec.emblemStyle)
            }
        }
    }
}

@Composable
fun PlayerAvatarView(
    avatarId: String,
    modifier: Modifier = Modifier,
    side: PieceColor = PieceColor.WHITE,
    size: Dp = 40.dp,
    pieceStyleId: String = "classic",
    cornerRadius: Dp = 12.dp,
) {
    val opt = PlayerAvatarCatalog.forId(avatarId)
    val primary = Color(opt.primaryHex)
    val secondary = Color(opt.secondaryHex)
    val accent = Color(opt.accentHex)

    Surface(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${opt.label} player avatar" },
        shape = RoundedCornerShape(cornerRadius),
        color = secondary,
        border = BorderStroke(1.6.dp, accent.copy(alpha = 0.9f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAvatarBackdrop(primary = primary, secondary = secondary, accent = accent, motif = opt.motif)
                val pad = this.size.minDimension * 0.15f
                drawChessPiece(
                    piece = Piece(opt.pieceType, side),
                    topLeft = Offset(pad, pad),
                    cellSize = this.size.minDimension - pad * 2f,
                    pieceStyleId = pieceStyleId,
                )
                drawAvatarForegroundAccent(accent = accent, motif = opt.motif)
            }
        }
    }
}

private fun DrawScope.drawAvatarBackdrop(
    primary: Color,
    secondary: Color,
    accent: Color,
    motif: String,
) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(primary, secondary),
            center = Offset(w * 0.35f, h * 0.28f),
            radius = w * 0.95f,
        ),
    )

    when (motif) {
        "cyber" -> {
            // Digital scanlines & corner circuit nodes
            for (i in 1..4) {
                val y = h * (i / 5f)
                drawLine(accent.copy(alpha = 0.22f), Offset(0f, y), Offset(w, y), strokeWidth = w * 0.025f)
            }
            drawRect(
                color = accent.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                style = Stroke(width = w * 0.03f),
            )
        }
        "flame", "bolt" -> {
            // Energetic diagonal slash aura
            drawLine(
                color = accent.copy(alpha = 0.32f),
                start = Offset(w * 0.1f, h * 0.9f),
                end = Offset(w * 0.9f, h * 0.1f),
                strokeWidth = w * 0.14f,
                cap = StrokeCap.Round,
            )
        }
        "aurora", "sparks" -> {
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = w * 0.36f,
                center = Offset(w * 0.72f, h * 0.26f),
            )
        }
        "laurel", "crown", "tiara" -> {
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = w * 0.42f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = w * 0.06f),
            )
        }
        "shield", "kabuto", "hood" -> {
            val shield = Path().apply {
                moveTo(w * 0.18f, h * 0.16f)
                lineTo(w * 0.82f, h * 0.16f)
                lineTo(w * 0.76f, h * 0.65f)
                lineTo(w * 0.50f, h * 0.88f)
                lineTo(w * 0.24f, h * 0.65f)
                close()
            }
            drawPath(shield, color = accent.copy(alpha = 0.16f))
        }
        else -> {
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = w * 0.38f,
                center = Offset(w * 0.5f, h * 0.5f),
            )
        }
    }
}

private fun DrawScope.drawAvatarForegroundAccent(
    accent: Color,
    motif: String,
) {
    val w = size.width
    val h = size.height
    when (motif) {
        "glasses" -> {
            val sw = (w * 0.045f).coerceAtLeast(1.2f)
            drawCircle(accent, radius = w * 0.11f, center = Offset(w * 0.37f, h * 0.22f), style = Stroke(sw))
            drawCircle(accent, radius = w * 0.11f, center = Offset(w * 0.63f, h * 0.22f), style = Stroke(sw))
            drawLine(accent, Offset(w * 0.48f, h * 0.22f), Offset(w * 0.52f, h * 0.22f), strokeWidth = sw)
        }
        "monocle" -> {
            val sw = (w * 0.045f).coerceAtLeast(1.2f)
            drawCircle(accent, radius = w * 0.11f, center = Offset(w * 0.66f, h * 0.25f), style = Stroke(sw))
            drawLine(accent, Offset(w * 0.77f, h * 0.29f), Offset(w * 0.85f, h * 0.50f), strokeWidth = sw * 0.7f)
        }
        "crown", "tiara" -> {
            val crown = Path().apply {
                moveTo(w * 0.32f, h * 0.20f)
                lineTo(w * 0.36f, h * 0.08f)
                lineTo(w * 0.50f, h * 0.15f)
                lineTo(w * 0.64f, h * 0.08f)
                lineTo(w * 0.68f, h * 0.20f)
                close()
            }
            drawPath(crown, color = accent)
        }
        "sprout" -> {
            drawCircle(accent, radius = w * 0.065f, center = Offset(w * 0.78f, h * 0.22f))
        }
        "diamond", "sparks" -> {
            val gem = Path().apply {
                moveTo(w * 0.80f, h * 0.10f)
                lineTo(w * 0.90f, h * 0.20f)
                lineTo(w * 0.80f, h * 0.30f)
                lineTo(w * 0.70f, h * 0.20f)
                close()
            }
            drawPath(gem, color = accent)
        }
        "bolt" -> {
            val bolt = Path().apply {
                moveTo(w * 0.80f, h * 0.10f)
                lineTo(w * 0.68f, h * 0.24f)
                lineTo(w * 0.78f, h * 0.24f)
                lineTo(w * 0.70f, h * 0.38f)
                lineTo(w * 0.86f, h * 0.20f)
                lineTo(w * 0.76f, h * 0.20f)
                close()
            }
            drawPath(bolt, color = accent)
        }
        "cyber" -> {
            drawCircle(accent, radius = w * 0.06f, center = Offset(w * 0.20f, h * 0.20f))
            drawCircle(accent, radius = w * 0.06f, center = Offset(w * 0.80f, h * 0.20f))
        }
    }
}
