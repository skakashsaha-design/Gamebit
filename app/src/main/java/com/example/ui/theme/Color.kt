package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Grandmaster Atelier Dark Palette
val ObsidianBg = Color(0xFF0B1117)
val ObsidianSurface = Color(0xFF131C26)
val ObsidianSurfaceVariant = Color(0xFF1C2733)
val ObsidianOutline = Color(0xFF2D3C4E)

val EmeraldPrimary = Color(0xFF10B981)
val EmeraldOnPrimary = Color(0xFF042218)
val EmeraldContainer = Color(0xFF0B3B2D)
val EmeraldOnContainer = Color(0xFFA7F3D0)

val AmberSecondary = Color(0xFFF59E0B)
val AmberOnSecondary = Color(0xFF2A1800)
val AmberContainer = Color(0xFF422802)
val AmberOnContainer = Color(0xFFFDE68A)

val SkyTertiary = Color(0xFF38BDF8)
val SkyOnTertiary = Color(0xFF002436)
val SkyContainer = Color(0xFF0C354A)
val SkyOnContainer = Color(0xFFBAE6FD)

val CoralError = Color(0xFFEF4444)
val CoralOnError = Color(0xFF2A0505)
val CoralContainer = Color(0xFF4B1113)
val CoralOnContainer = Color(0xFFFECACA)

// Warm Alabaster Light Palette
val AlabasterBg = Color(0xFFF7F6F2)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurfaceVariant = Color(0xFFECEAE3)
val AlabasterOutline = Color(0xFFCFCBC0)

val EmeraldLightPrimary = Color(0xFF059669)
val EmeraldLightContainer = Color(0xFFD1FAE5)
val EmeraldLightOnContainer = Color(0xFF064E3B)

val AmberLightSecondary = Color(0xFFD97706)
val AmberLightContainer = Color(0xFFFEF3C7)
val AmberLightOnContainer = Color(0xFF78350F)

val SkyLightTertiary = Color(0xFF0284C7)
val SkyLightContainer = Color(0xFFE0F2FE)
val SkyLightOnContainer = Color(0xFF0C4A6E)

// Move Classification Semantic Colors
val EvalBest = Color(0xFF10B981)
val EvalBrilliant = Color(0xFF06B6D4)
val EvalGood = Color(0xFF84CC16)
val EvalBook = Color(0xFFA855F7)
val EvalInaccuracy = Color(0xFFF59E0B)
val EvalMistake = Color(0xFFF97316)
val EvalBlunder = Color(0xFFEF4444)

data class BoardThemeColors(
    val id: String,
    val displayName: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val selectedSquare: Color,
    val lastMoveLight: Color,
    val lastMoveDark: Color,
    val premoveSquare: Color,
    val checkHighlight: Color,
)

val ChessBoardThemes: List<BoardThemeColors> = listOf(
    BoardThemeColors(
        id = "emerald_slate",
        displayName = "Emerald Slate",
        lightSquare = Color(0xFFE2E8F0),
        darkSquare = Color(0xFF47695E),
        selectedSquare = Color(0xAA10B981),
        lastMoveLight = Color(0x99F59E0B),
        lastMoveDark = Color(0x99D97706),
        premoveSquare = Color(0x9938BDF8),
        checkHighlight = Color(0xCCEF4444),
    ),
    BoardThemeColors(
        id = "classic_wood",
        displayName = "Classic Walnut",
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        selectedSquare = Color(0x996495ED),
        lastMoveLight = Color(0x99CDD26A),
        lastMoveDark = Color(0x99AAA23A),
        premoveSquare = Color(0x9938BDF8),
        checkHighlight = Color(0xCCDC2626),
    ),
    BoardThemeColors(
        id = "tournament_green",
        displayName = "Tournament Green",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        selectedSquare = Color(0x99F6F669),
        lastMoveLight = Color(0x99BACA44),
        lastMoveDark = Color(0x9995A830),
        premoveSquare = Color(0x990EA5E9),
        checkHighlight = Color(0xCCEF4444),
    ),
    BoardThemeColors(
        id = "midnight_blue",
        displayName = "Midnight Blue",
        lightSquare = Color(0xFFDDE6ED),
        darkSquare = Color(0xFF4A6FA5),
        selectedSquare = Color(0x9910B981),
        lastMoveLight = Color(0x9960A5FA),
        lastMoveDark = Color(0x993B82F6),
        premoveSquare = Color(0x99F59E0B),
        checkHighlight = Color(0xCCEF4444),
    ),
    BoardThemeColors(
        id = "high_contrast",
        displayName = "High Contrast",
        lightSquare = Color(0xFFFFFFFF),
        darkSquare = Color(0xFF1F2937),
        selectedSquare = Color(0xCC06B6D4),
        lastMoveLight = Color(0xCCFACC15),
        lastMoveDark = Color(0xCCCA8A04),
        premoveSquare = Color(0xCC38BDF8),
        checkHighlight = Color(0xFFFF0000),
    ),
)
