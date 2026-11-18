package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.data.local.AppSettings
import com.example.data.local.ChessLesson
import com.example.data.local.LessonProgressEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.PuzzlesAndLessonsCatalog
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.ChessBoardThemes
import com.example.viewmodel.CoordinateTrainerState
import com.example.viewmodel.PuzzleMode
import com.example.viewmodel.PuzzleSessionState

@Composable
fun LearnScreen(
    settings: AppSettings,
    puzzleState: PuzzleSessionState,
    puzzleAttempts: List<PuzzleAttemptEntity>,
    lessonProgress: List<LessonProgressEntity>,
    coordState: CoordinateTrainerState,
    onSelectPuzzleMode: (PuzzleMode, String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onSubmitPuzzleMove: (Move) -> Unit,
    onRetryPuzzle: () -> Unit,
    onNextPuzzle: () -> Unit,
    onRevealHint: () -> Unit,
    onCompleteLessonStep: (ChessLesson, Int) -> Unit,
    onStartCoordTrainer: (Boolean) -> Unit,
    onTapCoordSquare: (Square) -> Unit,
    onPracticeEndgameInPlay: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val boardTheme = remember(settings.boardThemeId, settings.highContrast) {
        if (settings.highContrast) ChessBoardThemes.last()
        else ChessBoardThemes.firstOrNull { it.id == settings.boardThemeId } ?: ChessBoardThemes.first()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Puzzles") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Lessons") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Endgames") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Coords") })
        }

        when (selectedTab) {
            0 -> PuzzlesContent(
                settings = settings,
                state = puzzleState,
                attempts = puzzleAttempts,
                boardTheme = boardTheme,
                onSelectMode = onSelectPuzzleMode,
                onSelectTheme = onSelectTheme,
                onSubmitMove = onSubmitPuzzleMove,
                onRetry = onRetryPuzzle,
                onNext = onNextPuzzle,
                onHint = onRevealHint,
            )
            1 -> LessonsContent(
                settings = settings,
                progressList = lessonProgress,
                boardTheme = boardTheme,
                onCompleteStep = onCompleteLessonStep,
            )
            2 -> EndgamesContent(
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
    attempts: List<PuzzleAttemptEntity>,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onSelectMode: (PuzzleMode, String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onSubmitMove: (Move) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onHint: () -> Unit,
) {
    var themeSearch by remember { mutableStateOf("") }
    var showDashboard by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "${state.currentPuzzle.title} (${state.currentPuzzle.rating})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val meta = when (state.mode) {
                        PuzzleMode.STREAK -> "Streak: ${state.streakCount} (Best ${settings.puzzleBestStreak})"
                        PuzzleMode.STORM, PuzzleMode.RACER -> "Score: ${state.stormScore} • ${state.stormRemainingSec}s left"
                        else -> "Your Puzzle Rating: ${settings.localPuzzleRating}"
                    }
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { showDashboard = !showDashboard }) {
                    Text(if (showDashboard) "Board" else "Stats")
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

            item {
                OutlinedTextField(
                    value = themeSearch,
                    onValueChange = { themeSearch = it },
                    label = { Text("Filter Tactical Themes (16)") },
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

@Composable
private fun EndgamesContent(
    onPracticeEndgame: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Endgame Practice Drills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Launch any key theoretical endgame directly against the ChessHub bot.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(PuzzlesAndLessonsCatalog.ENDGAMES, key = { it.id }) { eg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(eg.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(eg.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(eg.objective, style = MaterialTheme.typography.bodySmall)
                    Text("Tip: ${eg.tip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { onPracticeEndgame(eg.fen) }) {
                        Text("Practice vs Bot")
                    }
                }
            }
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
