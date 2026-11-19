package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.BoardArrow
import com.example.chess.ChessBoardState
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.data.local.AppSettings
import com.example.data.local.ChessLesson
import com.example.data.local.ChessPuzzleEntity
import com.example.data.local.EndgameExercise
import com.example.data.local.LessonProgressEntity
import com.example.data.local.OpeningDrill
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.data.local.TacticalPuzzle
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.ChessBoardThemes
import com.example.viewmodel.CoordinateTrainerState
import com.example.viewmodel.EndgameTrainerState
import com.example.viewmodel.OpeningDrillSessionState
import com.example.viewmodel.PuzzleMode
import com.example.viewmodel.PuzzleSessionState

@Composable
fun LearnScreen(
    settings: AppSettings,
    puzzleState: PuzzleSessionState,
    puzzleCollection: List<ChessPuzzleEntity>,
    puzzleAttempts: List<PuzzleAttemptEntity>,
    lessonProgress: List<LessonProgressEntity>,
    drillState: OpeningDrillSessionState,
    coordState: CoordinateTrainerState,
    endgameState: EndgameTrainerState = EndgameTrainerState(),
    onSelectPuzzleMode: (PuzzleMode, String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectDifficultyBand: (String) -> Unit,
    onSubmitPuzzleMove: (Move) -> Unit,
    onRetryPuzzle: () -> Unit,
    onNextPuzzle: () -> Unit,
    onRevealHint: () -> Unit,
    onToggleBookmarkPuzzle: () -> Unit,
    onCreateCustomPuzzle: (String, String, String, Int, String, String, String) -> Boolean,
    onDeleteCustomPuzzle: (String) -> Unit,
    onLoadSpecificPuzzle: (TacticalPuzzle) -> Unit,
    onCompleteLessonStep: (ChessLesson, Int) -> Unit,
    onStartDrill: (OpeningDrill) -> Unit,
    onSubmitDrillMove: (Move) -> Unit,
    onStartCoordTrainer: (Boolean) -> Unit,
    onTapCoordSquare: (Square) -> Unit,
    onSelectEndgameExercise: (EndgameExercise) -> Unit = {},
    onSubmitEndgameMove: (Move) -> Unit = {},
    onUndoEndgameMove: () -> Unit = {},
    onResetEndgameExercise: () -> Unit = {},
    onToggleEndgameCoach: (Boolean) -> Unit = {},
    onToggleEndgameBestArrow: () -> Unit = {},
    onAnalyzeEndgameNow: () -> Unit = {},
    onToggleMoveAnimation: (Boolean) -> Unit = {},
    onPracticeEndgameInPlay: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val boardTheme = remember(settings.boardThemeId, settings.highContrast) {
        if (settings.highContrast) ChessBoardThemes.last()
        else ChessBoardThemes.firstOrNull { it.id == settings.boardThemeId } ?: ChessBoardThemes.first()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Puzzles") }, modifier = Modifier.testTag("learn_tab_puzzles"))
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Opening Drills") }, modifier = Modifier.testTag("learn_tab_drills"))
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Lessons") }, modifier = Modifier.testTag("learn_tab_lessons"))
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Endgames") }, modifier = Modifier.testTag("learn_tab_endgames"))
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Coords") }, modifier = Modifier.testTag("learn_tab_coords"))
        }

        when (selectedTab) {
            0 -> PuzzlesContent(
                settings = settings,
                state = puzzleState,
                puzzleCollection = puzzleCollection,
                attempts = puzzleAttempts,
                boardTheme = boardTheme,
                onSelectMode = onSelectPuzzleMode,
                onSelectTheme = onSelectTheme,
                onSelectDifficultyBand = onSelectDifficultyBand,
                onSubmitMove = onSubmitPuzzleMove,
                onRetry = onRetryPuzzle,
                onNext = onNextPuzzle,
                onHint = onRevealHint,
                onToggleBookmark = onToggleBookmarkPuzzle,
                onCreateCustomPuzzle = onCreateCustomPuzzle,
                onDeleteCustomPuzzle = onDeleteCustomPuzzle,
                onLoadSpecificPuzzle = onLoadSpecificPuzzle,
            )
            1 -> OpeningDrillsContent(
                settings = settings,
                state = drillState,
                boardTheme = boardTheme,
                onSelectDrill = onStartDrill,
                onSubmitMove = onSubmitDrillMove,
            )
            2 -> LessonsContent(
                settings = settings,
                progressList = lessonProgress,
                boardTheme = boardTheme,
                onCompleteStep = onCompleteLessonStep,
            )
            3 -> EndgamesContent(
                settings = settings,
                state = endgameState,
                boardTheme = boardTheme,
                onSelectExercise = onSelectEndgameExercise,
                onSubmitMove = onSubmitEndgameMove,
                onUndo = onUndoEndgameMove,
                onReset = onResetEndgameExercise,
                onToggleCoach = onToggleEndgameCoach,
                onToggleBestArrow = onToggleEndgameBestArrow,
                onAnalyzeNow = onAnalyzeEndgameNow,
                onToggleMoveAnimation = onToggleMoveAnimation,
                onPracticeEndgame = onPracticeEndgameInPlay,
            )
            else -> CoordinateTrainerContent(
                settings = settings,
                state = coordState,
                boardTheme = boardTheme,
                onStart = onStartCoordTrainer,
                onTapSquare = onTapCoordSquare,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PuzzlesContent(
    settings: AppSettings,
    state: PuzzleSessionState,
    puzzleCollection: List<ChessPuzzleEntity>,
    attempts: List<PuzzleAttemptEntity>,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onSelectMode: (PuzzleMode, String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectDifficultyBand: (String) -> Unit,
    onSubmitMove: (Move) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onHint: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCreateCustomPuzzle: (String, String, String, Int, String, String, String) -> Boolean,
    onDeleteCustomPuzzle: (String) -> Unit,
    onLoadSpecificPuzzle: (TacticalPuzzle) -> Unit,
) {
    var themeSearch by remember { mutableStateOf("") }
    var showDashboard by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val hintArrows = remember(state.showHint, state.currentPuzzle.id, state.solutionIndex) {
        if (!state.showHint) emptyList()
        else {
            val uci = state.currentPuzzle.solutionUci.getOrNull(state.solutionIndex)
            val f = uci?.take(2)?.let { Square.fromAlgebraic(it) }
            val t = uci?.drop(2)?.take(2)?.let { Square.fromAlgebraic(it) }
            if (f != null && t != null) listOf(BoardArrow(f, t, 0xCCF59E0B)) else emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PuzzleMode.entries) { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { onSelectMode(mode, state.selectedTheme) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("puzzle_mode_${mode.name.lowercase()}"),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.currentPuzzle.title} (${state.currentPuzzle.rating})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val meta = when (state.mode) {
                        PuzzleMode.STREAK -> "Streak: ${state.streakCount} (Best ${settings.puzzleBestStreak})"
                        PuzzleMode.STORM, PuzzleMode.RACER -> "Score: ${state.stormScore} • ${state.stormRemainingSec}s left"
                        else -> "Your Puzzle Rating: ${settings.localPuzzleRating} • Library: ${puzzleCollection.size.coerceAtLeast(PuzzlesAndLessonsCatalog.OFFLINE_PUZZLES.size)}"
                    }
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("puzzle_bookmark_btn")) {
                        Icon(
                            imageVector = if (state.isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (state.isCurrentBookmarked) "Remove Bookmark" else "Bookmark Puzzle",
                            tint = if (state.isCurrentBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.testTag("create_puzzle_btn")) {
                        Icon(Icons.Default.Add, contentDescription = "Create Custom Puzzle")
                    }
                    OutlinedButton(onClick = { showDashboard = !showDashboard }) {
                        Text(if (showDashboard) "Board" else "Stats")
                    }
                }
            }
        }

        if (showDashboard) {
            item {
                PuzzleStatsDashboard(settings = settings, attempts = attempts)
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChessBoardView(
                        state = state.boardState,
                        legalMoves = state.legalMoves,
                        theme = boardTheme,
                        modifier = Modifier.widthIn(max = 500.dp),
                        flipped = state.humanColor == PieceColor.BLACK,
                        interactive = !state.solved && !state.failed,
                        allowBothSides = false,
                        humanSide = state.humanColor,
                        showCoordinates = settings.showCoordinates,
                        showLegalMoves = settings.showLegalMoves,
                        showLastMove = settings.showLastMove,
                        autoQueen = settings.autoQueen,
                        pieceStyleId = settings.pieceStyleId,
                        moveAnimation = settings.moveAnimation,
                        reducedAnimations = settings.reducedAnimations,
                        animationSpeed = settings.animationSpeed,
                        arrows = hintArrows,
                        onMoveSelected = onSubmitMove,
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        state.solved -> MaterialTheme.colorScheme.primaryContainer
                        state.failed -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = state.feedbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onHint, modifier = Modifier.weight(1f).testTag("puzzle_hint_btn")) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hint")
                    }
                    OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f).testTag("puzzle_retry_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                    Button(onClick = onNext, modifier = Modifier.weight(1f).testTag("puzzle_next_btn")) {
                        Text("Next")
                    }
                }
            }

            // Difficulty Band filter chips
            item {
                Text("Difficulty Rating Band", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "<1000", "1000-1399", "1400+").forEach { band ->
                        FilterChip(
                            selected = state.selectedDifficultyBand == band,
                            onClick = { onSelectDifficultyBand(band) },
                            label = { Text(band) },
                        )
                    }
                }
            }

            // 20 Checkmate Pattern Library selector when in CHECKMATE_PATTERNS mode
            if (state.mode == PuzzleMode.CHECKMATE_PATTERNS) {
                item {
                    Text(
                        "20 Classic Checkmate Patterns",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PuzzlesAndLessonsCatalog.CHECKMATE_PATTERN_NAMES.forEach { pattern ->
                            FilterChip(
                                selected = state.currentPuzzle.patternName == pattern,
                                onClick = { onSelectTheme(pattern) },
                                label = { Text(pattern) },
                            )
                        }
                    }
                }
            }

            // Custom / Mistake Puzzles manager list
            val customOrSaved = puzzleCollection.filter {
                (state.mode == PuzzleMode.MISTAKES && (it.source == "custom" || it.source == "mistake")) ||
                    (state.mode == PuzzleMode.BOOKMARKS && it.bookmarked)
            }
            if (customOrSaved.isNotEmpty()) {
                item {
                    Text(
                        if (state.mode == PuzzleMode.BOOKMARKS) "Bookmarked Puzzles (${customOrSaved.size})" else "My Custom & Mistake Puzzles (${customOrSaved.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(customOrSaved, key = { it.id }) { entity ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onLoadSpecificPuzzle(entity.toTacticalPuzzle()) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entity.title} (${entity.difficultyRating})", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${entity.themes} • Solved ${entity.solvedCount}/${entity.attemptCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (entity.source == "custom" || entity.source == "mistake") {
                                IconButton(onClick = { onDeleteCustomPuzzle(entity.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete puzzle")
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = themeSearch,
                    onValueChange = { themeSearch = it },
                    label = { Text("Filter Tactical Themes (17)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(6.dp))
                val themes = listOf("All") + PuzzlesAndLessonsCatalog.PUZZLE_THEMES.filter {
                    themeSearch.isBlank() || it.contains(themeSearch, ignoreCase = true)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    themes.forEach { t ->
                        FilterChip(
                            selected = state.selectedTheme == t,
                            onClick = { onSelectTheme(t) },
                            label = { Text(t) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomPuzzleDialog(
            defaultFen = state.boardState.toFen(),
            onDismiss = { showCreateDialog = false },
            onSave = { title, fen, sol, rating, theme, hint ->
                val ok = onCreateCustomPuzzle(title, fen, sol, rating, theme, hint, "Custom user-authored puzzle.")
                if (ok) showCreateDialog = false
                ok
            },
        )
    }
}

@Composable
private fun CreateCustomPuzzleDialog(
    defaultFen: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, String) -> Boolean,
) {
    var title by remember { mutableStateOf("") }
    var fen by remember { mutableStateOf(defaultFen) }
    var solutionUci by remember { mutableStateOf("") }
    var ratingText by remember { mutableStateOf("1250") }
    var theme by remember { mutableStateOf("Tactics") }
    var hint by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Puzzle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fen, onValueChange = { fen = it; errorMsg = null }, label = { Text("FEN Position") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                OutlinedTextField(value = solutionUci, onValueChange = { solutionUci = it; errorMsg = null }, label = { Text("Solution UCI (e.g. d5f7 g8h8)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ratingText, onValueChange = { ratingText = it.filter { c -> c.isDigit() } }, label = { Text("Rating") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = theme, onValueChange = { theme = it }, label = { Text("Theme") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = hint, onValueChange = { hint = it }, label = { Text("Hint (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                errorMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val ok = onSave(
                    title,
                    fen,
                    solutionUci,
                    ratingText.toIntOrNull() ?: 1250,
                    theme,
                    hint,
                )
                if (!ok) errorMsg = "Enter a valid FEN and at least one UCI solution move."
            }) { Text("Save Puzzle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun OpeningDrillsContent(
    settings: AppSettings,
    state: OpeningDrillSessionState,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onSelectDrill: (OpeningDrill) -> Unit,
    onSubmitMove: (Move) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Opening & Gambit Repertoire Drills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${state.drill.title} (${state.drill.eco}) • Play for ${state.drill.side}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PuzzlesAndLessonsCatalog.OPENING_DRILLS, key = { it.id }) { drill ->
                    FilterChip(
                        selected = state.drill.id == drill.id,
                        onClick = { onSelectDrill(drill) },
                        label = { Text(if (drill.isGambit) "${drill.title} (Gambit)" else drill.title) },
                    )
                }
            }
        }
        item {
            val sideColor = if (state.drill.side.equals("Black", ignoreCase = true)) PieceColor.BLACK else PieceColor.WHITE
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ChessBoardView(
                    state = state.boardState,
                    legalMoves = state.legalMoves,
                    theme = boardTheme,
                    modifier = Modifier.widthIn(max = 480.dp),
                    flipped = sideColor == PieceColor.BLACK,
                    interactive = !state.completed,
                    allowBothSides = false,
                    humanSide = sideColor,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    pieceStyleId = settings.pieceStyleId,
                    moveAnimation = settings.moveAnimation,
                    reducedAnimations = settings.reducedAnimations,
                    animationSpeed = settings.animationSpeed,
                    onMoveSelected = onSubmitMove,
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (state.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(state.feedback, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { onSelectDrill(state.drill) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Drill")
            }
        }
    }
}

@Composable
private fun PuzzleStatsDashboard(
    settings: AppSettings,
    attempts: List<PuzzleAttemptEntity>,
) {
    val total = attempts.size
    val solved = attempts.count { it.solved }
    val acc = if (total == 0) 0 else (solved * 100) / total
    val byTheme = attempts.groupBy { it.theme }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Puzzle Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rating: ${settings.localPuzzleRating}")
                Text("Solved: $solved / $total ($acc%)")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Best Streak: ${settings.puzzleBestStreak}")
                Text("Best Storm: ${settings.puzzleBestStorm}")
            }
            if (byTheme.isNotEmpty()) {
                Text("Theme Accuracy Breakdown", style = MaterialTheme.typography.labelLarge)
                byTheme.entries.take(8).forEach { (theme, list) ->
                    val ratio = list.count { it.solved }.toFloat() / list.size.coerceAtLeast(1)
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(theme, style = MaterialTheme.typography.bodySmall)
                            Text("${(ratio * 100).toInt()}% (${list.size})", style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonsContent(
    settings: AppSettings,
    progressList: List<LessonProgressEntity>,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onCompleteStep: (ChessLesson, Int) -> Unit,
) {
    var activeLesson by remember { mutableStateOf<ChessLesson?>(null) }
    var stepIdx by remember(activeLesson?.id) { mutableIntStateOf(0) }
    var stepFeedback by remember(activeLesson?.id, stepIdx) { mutableStateOf("") }

    val lesson = activeLesson
    if (lesson == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Structured Chess Courses (Offline Ready)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(PuzzlesAndLessonsCatalog.LESSONS, key = { it.id }) { item ->
                val prog = progressList.firstOrNull { it.lessonId == item.id }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        activeLesson = item
                        stepIdx = 0
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (prog?.completed == true) Icons.Default.CheckCircle else Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.level} • ${item.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${prog?.completedSteps ?: 0}/${item.steps.size}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    } else {
        val step = lesson.steps[stepIdx.coerceIn(0, lesson.steps.lastIndex)]
        val boardState = remember(step.fen) { ChessBoardState.fromFen(step.fen).getOrElse { ChessBoardState.standard() } }
        val legal = remember(boardState) { MoveGenerator.legalMoves(boardState) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${lesson.title} (${stepIdx + 1}/${lesson.steps.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { activeLesson = null }) { Text("All Lessons") }
                }
                Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(step.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChessBoardView(
                        state = boardState,
                        legalMoves = legal,
                        theme = boardTheme,
                        modifier = Modifier.widthIn(max = 480.dp),
                        interactive = step.expectedMoveUci != null,
                        allowBothSides = false,
                        humanSide = boardState.sideToMove,
                        showCoordinates = settings.showCoordinates,
                        showLegalMoves = settings.showLegalMoves,
                        pieceStyleId = settings.pieceStyleId,
                        moveAnimation = settings.moveAnimation,
                        reducedAnimations = settings.reducedAnimations,
                        animationSpeed = settings.animationSpeed,
                        onMoveSelected = { move ->
                            if (move.uci == step.expectedMoveUci) {
                                onCompleteStep(lesson, stepIdx)
                                if (stepIdx + 1 < lesson.steps.size) {
                                    stepIdx++
                                    stepFeedback = "Great move! Advancing to next step."
                                } else {
                                    stepFeedback = "Lesson Complete!"
                                }
                            } else {
                                stepFeedback = "Try again! Hint: ${step.hint}"
                            }
                        },
                    )
                }
            }
            if (stepFeedback.isNotBlank()) {
                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(stepFeedback, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { stepFeedback = step.hint }, modifier = Modifier.weight(1f)) { Text("Hint") }
                    Button(
                        onClick = {
                            onCompleteStep(lesson, stepIdx)
                            if (stepIdx + 1 < lesson.steps.size) stepIdx++ else activeLesson = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (stepIdx + 1 < lesson.steps.size) "Next Step" else "Finish")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EndgamesContent(
    settings: AppSettings,
    state: EndgameTrainerState,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onSelectExercise: (EndgameExercise) -> Unit,
    onSubmitMove: (Move) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onToggleCoach: (Boolean) -> Unit,
    onToggleBestArrow: () -> Unit,
    onAnalyzeNow: () -> Unit,
    onToggleMoveAnimation: (Boolean) -> Unit,
    onPracticeEndgame: (String) -> Unit,
) {
    val bestArrow = remember(state.showBestMoveArrow, state.coachReport?.bestMoveUci, state.exercise.id, state.boardState.positionKey()) {
        if (!state.showBestMoveArrow) emptyList()
        else {
            val uci = state.coachReport?.bestMoveUci ?: state.exercise.bestStartUci
            val f = Square.fromAlgebraic(uci.take(2))
            val t = Square.fromAlgebraic(uci.drop(2).take(2))
            if (f != null && t != null) listOf(BoardArrow(f, t, 0xCC10B981)) else emptyList()
        }
    }
    val slideEnabled = settings.moveAnimation && !settings.reducedAnimations

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Interactive Endgame Trainer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${state.exercise.title} • ${state.exercise.category}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                state.exercise.objective,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PuzzlesAndLessonsCatalog.ENDGAMES, key = { it.id }) { eg ->
                    FilterChip(
                        selected = state.exercise.id == eg.id,
                        onClick = { onSelectExercise(eg) },
                        label = { Text(eg.title) },
                        modifier = Modifier.testTag("endgame_chip_${eg.id}"),
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ChessBoardView(
                    state = state.boardState,
                    legalMoves = state.legalMoves,
                    theme = boardTheme,
                    modifier = Modifier.widthIn(max = 500.dp),
                    flipped = state.humanColor == PieceColor.BLACK,
                    interactive = !state.gameStatus.isTerminal && !state.isBotThinking,
                    allowBothSides = false,
                    humanSide = state.humanColor,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    pieceStyleId = settings.pieceStyleId,
                    moveAnimation = settings.moveAnimation,
                    reducedAnimations = settings.reducedAnimations,
                    animationSpeed = settings.animationSpeed,
                    lastMove = state.lastMove,
                    arrows = bestArrow,
                    onMoveSelected = onSubmitMove,
                )
            }
        }

        item {
            val statusColor = when {
                state.gameStatus.isTerminal -> MaterialTheme.colorScheme.primaryContainer
                state.coachReport?.moveVerdict?.isMistake == true -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = statusColor,
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (state.moveHistorySan.isNotEmpty()) {
                        val moveLine = state.moveHistorySan.chunked(2).mapIndexed { i, pair ->
                            if (pair.size > 1) "${i + 1}. ${pair[0]} ${pair[1]}" else "${i + 1}. ${pair[0]}"
                        }.joinToString("  ")
                        Text(
                            text = moveLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = state.coachEnabled,
                    onClick = { onToggleCoach(!state.coachEnabled) },
                    label = { Text("Coach Explain") },
                    leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("endgame_coach_toggle"),
                )
                FilterChip(
                    selected = slideEnabled,
                    onClick = { onToggleMoveAnimation(!slideEnabled) },
                    label = { Text("Slide Anim") },
                    leadingIcon = { Icon(Icons.Default.Animation, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("endgame_slide_toggle"),
                )
                FilterChip(
                    selected = state.showBestMoveArrow,
                    onClick = onToggleBestArrow,
                    label = { Text("Best Arrow") },
                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("endgame_best_arrow_toggle"),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = state.canUndo && !state.isBotThinking,
                    modifier = Modifier.weight(1f).testTag("endgame_undo_btn"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).testTag("endgame_reset_btn"),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
                FilledTonalButton(
                    onClick = { onPracticeEndgame(state.boardState.toFen()) },
                    modifier = Modifier.weight(1.2f).testTag("endgame_practice_full_btn"),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play Full")
                }
            }
        }

        item {
            AnimatedVisibility(visible = state.coachEnabled) {
                val report = state.coachReport
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("endgame_coach_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Endgame Coach Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    report?.classificationTitle ?: state.exercise.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = report?.evalFormatted ?: "--",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                TextButton(
                                    onClick = onAnalyzeNow,
                                    enabled = !state.isAnalyzing,
                                    modifier = Modifier.testTag("endgame_analyze_now_btn"),
                                ) {
                                    if (state.isAnalyzing) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Analyze")
                                    }
                                }
                            }
                        }

                        if (report == null) {
                            Text(
                                if (state.isAnalyzing) "Analyzing endgame structure and best continuation..."
                                else "Tap Analyze or play a move for Mistake, Weak Point, Advantage, and Tips breakdown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            HorizontalDivider()

                            // 1. Mistake
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    val verdict = report.moveVerdict
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (verdict?.isMistake == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Mistake & Move Check",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (verdict != null) {
                                        val badgeColor = when (verdict.quality) {
                                            MoveQuality.BLUNDER, MoveQuality.MISTAKE -> MaterialTheme.colorScheme.errorContainer
                                            MoveQuality.INACCURACY -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = badgeColor) {
                                            val deltaStr = if (verdict.cpLoss > 0) " (-${String.format(java.util.Locale.US, "%.2f", verdict.cpLoss / 100.0)})" else ""
                                            Text(
                                                "${verdict.quality.label}: ${verdict.playedSan}$deltaStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(report.mistakeExplanation, style = MaterialTheme.typography.bodySmall)
                                if (report.moveVerdict?.isMistake == true) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onUndo, enabled = state.canUndo) {
                                            Text("Undo Move")
                                        }
                                        if (!state.showBestMoveArrow) {
                                            TextButton(onClick = onToggleBestArrow) {
                                                Text("Show Best (${report.moveVerdict.bestSan})")
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            // 2. Weak Point
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(
                                        "Weak Points",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                report.weakPoints.forEach { wp ->
                                    Text("• $wp", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            HorizontalDivider()

                            // 3. Advantage
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Advantage",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(report.advantageSummary, style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider()

                            // 4. Tips
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Default.TipsAndUpdates,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                    Text(
                                        "Tips",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                report.tips.forEach { tip ->
                                    Text("• $tip", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CoordinateTrainerContent(
    settings: AppSettings,
    state: CoordinateTrainerState,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onStart: (Boolean) -> Unit,
    onTapSquare: (Square) -> Unit,
) {
    var flipped by remember { mutableStateOf(false) }
    val emptyBoard = remember { ChessBoardState.fromFen("8/8/8/8/8/8/8/8 w - - 0 1").getOrThrow() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Coordinate Trainer (30s)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Score: ${state.score}/${state.attempts} • ${state.timeRemainingSec}s", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { flipped = !flipped }) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip board")
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (state.lastResultCorrect) {
                    true -> MaterialTheme.colorScheme.primaryContainer
                    false -> MaterialTheme.colorScheme.errorContainer
                    null -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = if (state.active) "Tap square: ${state.targetSquare.uppercase()}" else "Press Start to train board coordinates",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }
        }
        item {
            ChessBoardView(
                state = emptyBoard,
                legalMoves = emptyList(),
                theme = boardTheme,
                modifier = Modifier.widthIn(max = 460.dp),
                flipped = flipped,
                interactive = state.active,
                showCoordinates = false,
                showLegalMoves = false,
                pieceStyleId = settings.pieceStyleId,
                onSquareTappedRaw = onTapSquare,
            )
        }
        item {
            Button(
                onClick = { onStart(flipped) },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_coord_trainer_btn"),
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.active) "Restart 30s Sprint" else "Start 30s Sprint")
            }
        }
    }
}
