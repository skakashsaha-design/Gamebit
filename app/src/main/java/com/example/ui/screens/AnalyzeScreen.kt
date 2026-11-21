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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.BoardHighlight
import com.example.chess.CastlingRights
import com.example.chess.ChessBoardState
import com.example.chess.ChessVariant
import com.example.chess.MasterGamesCatalog
import com.example.chess.Move
import com.example.chess.MoveGenerator
import com.example.chess.MoveNode
import com.example.chess.MoveQuality
import com.example.chess.OpeningBook
import com.example.chess.PgnParser
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.ReviewedMove
import com.example.chess.Square
import com.example.data.local.AnimationMode
import com.example.data.local.AppSettings
import com.example.data.local.CollectionEntity
import com.example.data.local.LocalGameEntity
import com.example.data.local.LocalStudyEntity
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.EvaluationGraph
import com.example.ui.components.EvaluationPill
import com.example.ui.components.GamebitDialog
import com.example.ui.components.MoveExplanationDialog
import com.example.ui.components.MoveQualityBadge
import com.example.ui.components.PieceMiniIcon
import com.example.ui.theme.ChessBoardThemes
import com.example.viewmodel.AnalysisUiState
import kotlin.math.abs

@Composable
fun AnalyzeScreen(
    state: AnalysisUiState,
    settings: AppSettings,
    isOnline: Boolean,
    savedGames: List<LocalGameEntity>,
    studies: List<LocalStudyEntity>,
    collections: List<CollectionEntity>,
    onPlayMove: (Move) -> Unit,
    onToggleEngine: () -> Unit,
    onToggleFlip: () -> Unit,
    onJumpStart: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpEnd: () -> Unit,
    onJumpToPly: (Int) -> Unit,
    onSelectNode: (MoveNode) -> Unit,
    onToggleAutoplay: () -> Unit,
    onSetComment: (String) -> Unit,
    onToggleNag: (Int) -> Unit,
    onToggleCircleSquare: (Square) -> Unit,
    onToggleUserArrow: (Square, Square) -> Unit,
    onClearAnnotations: () -> Unit,
    onRunUciCommand: (String) -> Unit,
    onRunUciBenchmark: () -> Unit,
    onRestartUciEngine: () -> Unit,
    onSaveCurrentAsPuzzle: (String, String, String, Int, String) -> Unit,
    onRunFullReview: () -> Unit,
    onFetchExplorer: (Boolean) -> Unit,
    onQueryTablebase: () -> Unit,
    onLoadFen: (String) -> Boolean,
    onImportPgn: (String) -> Boolean,
    onExportPgn: () -> String,
    onSaveStudy: (String, String) -> Unit,
    onPublishStudyCloud: (String, String, String) -> Unit,
    onLoadSavedGame: (LocalGameEntity) -> Unit,
    onToggleFavoriteGame: (LocalGameEntity) -> Unit,
    onDeleteGame: (String) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onPlayFromPosition: (String) -> Unit,
    onShareText: (String, String) -> Unit,
    onStopEngine: () -> Unit = {},
    onSetAutoplaySpeed: (Float) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val boardTheme = remember(settings.boardThemeId, settings.highContrast) {
        if (settings.highContrast) ChessBoardThemes.last()
        else ChessBoardThemes.firstOrNull { it.id == settings.boardThemeId } ?: ChessBoardThemes.first()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Analysis") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Game Review") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Explore") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Board Editor") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Archive (${savedGames.size})") })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Studies (${studies.size})") })
        }

        when (selectedTab) {
            0 -> AnalysisBoardTab(
                state = state,
                settings = settings,
                boardTheme = boardTheme,
                onPlayMove = onPlayMove,
                onToggleEngine = onToggleEngine,
                onStopEngine = onStopEngine,
                onToggleFlip = onToggleFlip,
                onJumpStart = onJumpStart,
                onStepBack = onStepBack,
                onStepForward = onStepForward,
                onJumpEnd = onJumpEnd,
                onJumpToPly = onJumpToPly,
                onSelectNode = onSelectNode,
                onToggleAutoplay = onToggleAutoplay,
                onSetAutoplaySpeed = onSetAutoplaySpeed,
                onSetComment = onSetComment,
                onToggleNag = onToggleNag,
                onToggleCircleSquare = onToggleCircleSquare,
                onToggleUserArrow = onToggleUserArrow,
                onClearAnnotations = onClearAnnotations,
                onRunUciCommand = onRunUciCommand,
                onRunUciBenchmark = onRunUciBenchmark,
                onRestartUciEngine = onRestartUciEngine,
                onSaveCurrentAsPuzzle = onSaveCurrentAsPuzzle,
                onFetchExplorer = onFetchExplorer,
                onQueryTablebase = onQueryTablebase,
                onSaveStudy = onSaveStudy,
                onShareText = onShareText,
                onExportPgn = onExportPgn,
            )
            1 -> GameReviewTab(
                state = state,
                settings = settings,
                boardTheme = boardTheme,
                onRunReview = onRunFullReview,
                onJumpToPly = onJumpToPly,
                onPlayMove = onPlayMove,
                onPlayFromPosition = onPlayFromPosition,
            )
            2 -> ExploreMasterAndGambitsTab(
                onLoadPgn = { pgn ->
                    if (onImportPgn(pgn)) selectedTab = 0
                },
            )
            3 -> BoardEditorTab(
                initialFen = state.currentNode.state.toFen(),
                settings = settings,
                boardTheme = boardTheme,
                onLoadToAnalysis = { fen ->
                    if (onLoadFen(fen)) selectedTab = 0
                },
                onPlayFromFen = onPlayFromPosition,
                onShareText = onShareText,
            )
            4 -> ArchiveTab(
                savedGames = savedGames,
                onLoadGame = { g ->
                    onLoadSavedGame(g)
                    selectedTab = 0
                },
                onToggleFavorite = onToggleFavoriteGame,
                onDeleteGame = onDeleteGame,
                onImportPgn = { pgn ->
                    val ok = onImportPgn(pgn)
                    if (ok) selectedTab = 0
                    ok
                },
                onLoadFen = { fen ->
                    val ok = onLoadFen(fen)
                    if (ok) selectedTab = 0
                    ok
                },
                onShareText = onShareText,
            )
            else -> StudiesAndCollectionsTab(
                studies = studies,
                collections = collections,
                isOnline = isOnline,
                onLoadStudyPgn = { pgn ->
                    if (onImportPgn(pgn)) selectedTab = 0
                },
                onPublishCloud = onPublishStudyCloud,
                onCreateCollection = onCreateCollection,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalysisBoardTab(
    state: AnalysisUiState,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onPlayMove: (Move) -> Unit,
    onToggleEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onToggleFlip: () -> Unit,
    onJumpStart: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpEnd: () -> Unit,
    onJumpToPly: (Int) -> Unit,
    onSelectNode: (MoveNode) -> Unit,
    onToggleAutoplay: () -> Unit,
    onSetAutoplaySpeed: (Float) -> Unit,
    onSetComment: (String) -> Unit,
    onToggleNag: (Int) -> Unit,
    onToggleCircleSquare: (Square) -> Unit,
    onToggleUserArrow: (Square, Square) -> Unit,
    onClearAnnotations: () -> Unit,
    onRunUciCommand: (String) -> Unit,
    onRunUciBenchmark: () -> Unit,
    onRestartUciEngine: () -> Unit,
    onSaveCurrentAsPuzzle: (String, String, String, Int, String) -> Unit,
    onFetchExplorer: (Boolean) -> Unit,
    onQueryTablebase: () -> Unit,
    onSaveStudy: (String, String) -> Unit,
    onShareText: (String, String) -> Unit,
    onExportPgn: () -> String,
) {
    var commentDraft by remember(state.currentNode.id) { mutableStateOf(state.currentNode.comment) }
    var studyTitle by remember { mutableStateOf("") }
    var annotationMode by remember { mutableStateOf(false) }
    var showUciConsole by remember { mutableStateOf(false) }
    var uciCommandInput by remember { mutableStateOf("go depth 2") }
    var showSavePuzzleDialog by remember { mutableStateOf(false) }
    var inspectedMove by remember { mutableStateOf<ReviewedMove?>(null) }

    val reduceMotion = settings.reducedAnimations || settings.lowMemoryMode || settings.animationMode != AnimationMode.FULL
    val reviewedByPly = remember(state.reviewReport) {
        state.reviewReport?.moves?.associateBy { it.ply }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = state.openingMatch?.let { "${it.name} (${it.eco})" } ?: "Analysis Board",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (state.openingMatch != null && state.currentNode.ply <= 16) {
                            MoveQualityBadge(quality = MoveQuality.BOOK, compact = true)
                        }
                    }
                    state.gambitMatch?.let { g ->
                        Text(
                            text = "Gambit: ${g.name} (${g.status})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    state.endgameClassification?.let { eg ->
                        Text(
                            text = "Endgame: ${eg.category} • ${eg.detail}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.engineEnabled || state.isAutoPlaying) {
                        OutlinedButton(
                            onClick = onStopEngine,
                            modifier = Modifier.testTag("stop_engine_btn"),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    Text("Engine", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = state.engineEnabled,
                        onCheckedChange = { onToggleEngine() },
                        modifier = Modifier.testTag("analysis_engine_switch"),
                    )
                }
            }
        }

        // Board + Evaluation Bar side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (settings.showEvalBar) {
                    EvaluationBar(
                        topLine = state.engineLines.firstOrNull(),
                        flipped = state.flipped,
                        modifier = Modifier.height(290.dp).padding(end = 6.dp),
                    )
                }
                ChessBoardView(
                    state = state.currentNode.state,
                    legalMoves = state.legalMoves,
                    theme = boardTheme,
                    modifier = Modifier.weight(1f).widthIn(max = 460.dp),
                    flipped = state.flipped,
                    interactive = true,
                    allowBothSides = true,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    pieceStyleId = settings.pieceStyleId,
                    moveAnimation = settings.moveAnimation,
                    reducedAnimations = settings.reducedAnimations,
                    animationMode = settings.animationMode,
                    animationSpeed = settings.animationSpeed,
                    annotationMode = annotationMode && settings.allowBoardArrows,
                    lastMove = state.currentNode.move,
                    arrows = state.arrows + state.userArrows,
                    highlights = state.userCircleSquares.map { BoardHighlight(it, 0xCC10B981) },
                    onMoveSelected = onPlayMove,
                    onToggleCircleSquare = onToggleCircleSquare,
                    onToggleUserArrow = onToggleUserArrow,
                )
            }
        }

        // Evaluation Pill
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EvaluationPill(topLine = state.engineLines.firstOrNull())
            }
        }

        // Navigation controls & Autoplay Speed Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onJumpStart) { Icon(Icons.Default.FirstPage, contentDescription = "Start") }
                    IconButton(onClick = onStepBack) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back") }
                    IconButton(onClick = onToggleAutoplay) {
                        Icon(if (state.isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Autoplay")
                    }
                    IconButton(onClick = onStepForward) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Forward") }
                    IconButton(onClick = onJumpEnd) { Icon(Icons.Default.LastPage, contentDescription = "End") }
                    IconButton(onClick = onToggleFlip) { Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Speed:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    listOf(
                        0.5f to ("0.5×" to "autoplay_speed_0.5x"),
                        1.0f to ("1×" to "autoplay_speed_1x"),
                        2.0f to ("2×" to "autoplay_speed_2x"),
                    ).forEach { (mult, labelAndTag) ->
                        FilterChip(
                            selected = abs(state.autoplayMultiplier - mult) < 0.05f,
                            onClick = { onSetAutoplaySpeed(mult) },
                            label = { Text(labelAndTag.first) },
                            modifier = Modifier.padding(horizontal = 3.dp).testTag(labelAndTag.second),
                        )
                    }
                }
            }
        }

        // Compact Paired Move List (2-column pairs, active highlight, quality badges, click-to-jump + Why?)
        item {
            val mainNodes = state.rootNode.mainLine()
            if (mainNodes.isNotEmpty()) {
                val pairs = remember(mainNodes) { mainNodes.chunked(2) }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("analysis_paired_move_list"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Move List (tap to jump, tap badge for why)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        pairs.forEachIndexed { pairIdx, pair ->
                            val whiteNode = pair[0]
                            val blackNode = pair.getOrNull(1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${pairIdx + 1}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp),
                                )
                                AnalysisMoveCell(
                                    node = whiteNode,
                                    isSelected = whiteNode.id == state.currentNode.id,
                                    reviewedMove = reviewedByPly[whiteNode.ply],
                                    onSelect = { onSelectNode(whiteNode) },
                                    onInspect = { rm -> inspectedMove = rm },
                                    modifier = Modifier.weight(1f),
                                )
                                if (blackNode != null) {
                                    AnalysisMoveCell(
                                        node = blackNode,
                                        isSelected = blackNode.id == state.currentNode.id,
                                        reviewedMove = reviewedByPly[blackNode.ply],
                                        onSelect = { onSelectNode(blackNode) },
                                        onInspect = { rm -> inspectedMove = rm },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Evaluation Graph directly below move list when a review exists
        state.reviewReport?.let { rep ->
            if (rep.moves.isNotEmpty()) {
                item {
                    EvaluationGraph(
                        reviewedMoves = rep.moves,
                        selectedPly = state.currentNode.ply,
                        onSelectPly = onJumpToPly,
                        reducedAnimations = reduceMotion,
                    )
                }
            }
        }

        // Annotations, Save Puzzle, & UCI Console Quick Actions
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (settings.allowBoardArrows) {
                    FilterChip(
                        selected = annotationMode,
                        onClick = { annotationMode = !annotationMode },
                        leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (annotationMode) "Annotating Board" else "Draw Arrows/Circles") },
                    )
                    if (state.userArrows.isNotEmpty() || state.userCircleSquares.isNotEmpty()) {
                        OutlinedButton(onClick = onClearAnnotations) { Text("Clear Marks") }
                    }
                }
                OutlinedButton(onClick = { showSavePuzzleDialog = true }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save as Puzzle")
                }
                FilterChip(
                    selected = showUciConsole,
                    onClick = { showUciConsole = !showUciConsole },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("UCI Console") },
                    modifier = Modifier.testTag("toggle_uci_console_btn"),
                )
            }
        }

        // Collapsible Real UCI Protocol Engine Console
        if (showUciConsole) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "ChessHub Kotlin UCI Engine 2.0 (${if (state.uciRunning) "Ready" else "Stopped"})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = onRestartUciEngine) { Text("Restart UCI") }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = onRunUciBenchmark) { Text("Analyze Position") }
                            OutlinedButton(onClick = { onRunUciCommand("uci") }) { Text("uci") }
                            OutlinedButton(onClick = { onRunUciCommand("isready") }) { Text("isready") }
                            OutlinedButton(onClick = { onRunUciCommand("stop") }) { Text("stop") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uciCommandInput,
                                onValueChange = { uciCommandInput = it },
                                label = { Text("UCI Command") },
                                modifier = Modifier.weight(1f).testTag("uci_command_input"),
                                singleLine = true,
                            )
                            Button(onClick = {
                                if (uciCommandInput.isNotBlank()) onRunUciCommand(uciCommandInput)
                            }) { Text("Send") }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                val lines = if (state.uciOutput.isEmpty()) listOf("id name ChessHub Kotlin UCI Engine 2.0 (tap 'Analyze Position' or send 'uci')") else state.uciOutput.takeLast(12)
                                lines.forEach { line ->
                                    Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi-PV Engine Lines & Telemetry
        if (state.engineLines.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val top = state.engineLines.first()
                        Text(
                            text = "Local Engine • Depth ${top.depth} • ${top.nodes} nodes • ${top.nps} nps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        state.engineLines.forEach { line ->
                            Text(
                                text = "${line.multipvRank}. [${line.formattedScore}] ${line.pvSan.take(6).joinToString(" ")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }

        if (state.coachInsight.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(state.coachInsight, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Variation Tree Move Strip
        item {
            val mainNodes = state.rootNode.mainLine()
            if (mainNodes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(mainNodes, key = { it.id }) { node ->
                        val isSel = node.id == state.currentNode.id
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectNode(node) },
                            label = { Text("${(node.ply + 1) / 2}.${if (node.ply % 2 == 0) ".." else ""}${node.san}") },
                        )
                    }
                }
            }
        }

        // Comments & NAG annotations
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(1 to "!", 2 to "?", 3 to "!!", 4 to "??", 5 to "!?", 6 to "?!").forEach { (nag, sym) ->
                    FilterChip(
                        selected = nag in state.currentNode.nags,
                        onClick = { onToggleNag(nag) },
                        label = { Text(sym) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    label = { Text("Move Comment") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalButton(onClick = { onSetComment(commentDraft) }) { Text("Save") }
            }
        }

        // Local Book & Online Explorer + Syzygy Tablebase
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Opening Explorer & Tablebase", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (state.localBookMoves.isNotEmpty()) {
                        Text("Local ECO Book:", style = MaterialTheme.typography.labelMedium)
                        state.localBookMoves.take(4).forEach { bm ->
                            Text(
                                text = "${bm.san} (${bm.games} games • W${bm.whiteWinPct}%/D${bm.drawPct}%/B${bm.blackWinPct}%) — ${bm.openingName}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onFetchExplorer(true) }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Masters DB")
                        }
                        OutlinedButton(onClick = { onFetchExplorer(false) }) { Text("Lichess DB") }
                        OutlinedButton(onClick = onQueryTablebase) { Text("Syzygy 7-Piece") }
                    }
                    if (state.externalExplorerLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    state.externalExplorer?.let { exp ->
                        Text("${exp.eco}: ${exp.openingName}", style = MaterialTheme.typography.labelLarge)
                        exp.moves.take(4).forEach { m ->
                            Text("${m.san} (${m.totalGames} games, avg ${m.averageRating})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    state.tablebaseResult?.let { tb ->
                        Text(
                            "Tablebase: ${tb.category} • Best: ${tb.bestMoveSan ?: "-"} • DTM ${tb.dtm ?: "-"} / DTZ ${tb.dtz ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    state.errorText?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Save Study & Share PGN/FEN
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = studyTitle,
                    onValueChange = { studyTitle = it },
                    label = { Text("Study Title") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalButton(onClick = {
                    onSaveStudy(studyTitle, state.openingMatch?.name ?: "Analysis")
                    studyTitle = ""
                }) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Study")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = { onShareText("Gamebit PGN", onExportPgn()) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share PGN")
                }
                OutlinedButton(onClick = { onShareText("Gamebit FEN", state.currentNode.state.toFen()) }, modifier = Modifier.weight(1f)) {
                    Text("Share FEN")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    inspectedMove?.let { rm ->
        MoveExplanationDialog(
            reviewedMove = rm,
            onDismiss = { inspectedMove = null },
        )
    }

    if (showSavePuzzleDialog) {
        val suggestedMove = state.engineLines.firstOrNull()?.pvMoves?.firstOrNull()?.uci ?: ""
        var title by remember { mutableStateOf(state.openingMatch?.name?.let { "$it Tactic" } ?: "Analysis Tactic") }
        var solutionUci by remember { mutableStateOf(suggestedMove) }
        var theme by remember { mutableStateOf("Tactical") }
        var ratingText by remember { mutableStateOf("1400") }
        var hint by remember { mutableStateOf("Look for checks, captures, and undefended targets.") }

        GamebitDialog(
            onDismissRequest = { showSavePuzzleDialog = false },
            title = "Save Position as Puzzle",
            subtitle = "Store this tactical position in your local puzzle trainer",
            actions = {
                TextButton(onClick = { showSavePuzzleDialog = false }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onSaveCurrentAsPuzzle(title, solutionUci, theme, ratingText.toIntOrNull() ?: 1400, hint)
                    showSavePuzzleDialog = false
                }) { Text("Save") }
            },
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Puzzle Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = solutionUci, onValueChange = { solutionUci = it }, label = { Text("Solution UCI (e.g. e2e4 or e2e4 e7e5)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = theme, onValueChange = { theme = it }, label = { Text("Theme") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ratingText, onValueChange = { ratingText = it.filter(Char::isDigit) }, label = { Text("Rating") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = hint, onValueChange = { hint = it }, label = { Text("Hint") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AnalysisMoveCell(
    node: MoveNode,
    isSelected: Boolean,
    reviewedMove: ReviewedMove?,
    onSelect: () -> Unit,
    onInspect: (ReviewedMove) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = node.san,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
            if (reviewedMove != null) {
                MoveQualityBadge(
                    quality = reviewedMove.quality,
                    compact = true,
                    modifier = Modifier.clickable {
                        onSelect()
                        onInspect(reviewedMove)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameReviewTab(
    state: AnalysisUiState,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onRunReview: () -> Unit,
    onJumpToPly: (Int) -> Unit,
    onPlayMove: (Move) -> Unit,
    onPlayFromPosition: (String) -> Unit,
) {
    var moveFilter by remember { mutableStateOf("All") }
    var inspectedMove by remember { mutableStateOf<ReviewedMove?>(null) }
    val reduceMotion = settings.reducedAnimations || settings.lowMemoryMode || settings.animationMode != AnimationMode.FULL

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Button(
                onClick = onRunReview,
                enabled = !state.isReviewing,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_game_review_btn"),
            ) {
                if (state.isReviewing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing Game...")
                } else {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run Full Game Review & Accuracy")
                }
            }
        }

        val report = state.reviewReport
        if (report == null) {
            item {
                Text(
                    "Import a PGN, load a game from Archive, or play moves on the Analysis board, then run Game Review for accuracy percentages, phase breakdown, ACPL, turning points, and full mistake explanations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                val wOp = report.whiteOpeningAccuracy?.let { "$it%" } ?: "--"
                val wMid = report.whiteMiddlegameAccuracy?.let { "$it%" } ?: "--"
                val wEnd = report.whiteEndgameAccuracy?.let { "$it%" } ?: "--"
                val bOp = report.blackOpeningAccuracy?.let { "$it%" } ?: "--"
                val bMid = report.blackMiddlegameAccuracy?.let { "$it%" } ?: "--"
                val bEnd = report.blackEndgameAccuracy?.let { "$it%" } ?: "--"

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("game_review_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Accuracy & Comparison Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("White: ${report.whiteAccuracy}%", fontWeight = FontWeight.Bold)
                                Text("ACPL: ${report.whiteAcpl} cp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Black: ${report.blackAccuracy}%", fontWeight = FontWeight.Bold)
                                Text("ACPL: ${report.blackAcpl} cp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "White Phases — Opening $wOp • Middlegame $wMid • Endgame $wEnd",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Black Phases — Opening $bOp • Middlegame $bMid • Endgame $bEnd",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (report.advantageSummary.isNotBlank()) {
                            Text(
                                text = report.advantageSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "White: ${report.whiteQualityCounts[MoveQuality.BRILLIANT] ?: 0} !! • ${report.whiteQualityCounts[MoveQuality.GREAT] ?: 0} ! • ${report.whiteInaccuracies} Inacc • ${report.whiteMistakes} Mist • ${report.whiteBlunders} Blund • ${report.whiteMissedWins} Miss • ${report.whiteMissedMates} Missed Mate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Black: ${report.blackQualityCounts[MoveQuality.BRILLIANT] ?: 0} !! • ${report.blackQualityCounts[MoveQuality.GREAT] ?: 0} ! • ${report.blackInaccuracies} Inacc • ${report.blackMistakes} Mist • ${report.blackBlunders} Blund • ${report.blackMissedWins} Miss • ${report.blackMissedMates} Missed Mate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        report.whiteBestMove?.let { m ->
                            Text("White Best: ${m.san} (${m.quality.label})", style = MaterialTheme.typography.labelSmall)
                        }
                        report.blackBestMove?.let { m ->
                            Text("Black Best: ${m.san} (${m.quality.label})", style = MaterialTheme.typography.labelSmall)
                        }
                        report.whiteWorstMove?.let { m ->
                            Text("White Critical Error: ${m.san} (-${m.cpLoss} cp)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        report.blackWorstMove?.let { m ->
                            Text("Black Critical Error: ${m.san} (-${m.cpLoss} cp)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        report.whiteMissedOpportunity?.let { m ->
                            Text("White Missed Opportunity: ${m.san} (Best ${m.bestMoveSan})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        report.blackMissedOpportunity?.let { m ->
                            Text("Black Missed Opportunity: ${m.san} (Best ${m.bestMoveSan})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            if (report.turningPoints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("turning_points_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Key Turning Points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            report.turningPoints.forEach { tp ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onJumpToPly(tp.ply) }.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${(tp.ply + 1) / 2}${if (tp.ply % 2 == 1) "." else "..."} ${tp.san} — ${tp.title}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        Text(tp.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    MoveQualityBadge(quality = tp.quality, compact = true)
                                }
                            }
                        }
                    }
                }
            }

            item {
                EvaluationGraph(
                    reviewedMoves = report.moves,
                    selectedPly = state.currentNode.ply,
                    onSelectPly = onJumpToPly,
                    reducedAnimations = reduceMotion,
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChessBoardView(
                        state = state.currentNode.state,
                        legalMoves = state.legalMoves,
                        theme = boardTheme,
                        modifier = Modifier.widthIn(max = 380.dp),
                        showCoordinates = settings.showCoordinates,
                        pieceStyleId = settings.pieceStyleId,
                        moveAnimation = settings.moveAnimation,
                        reducedAnimations = settings.reducedAnimations,
                        animationMode = settings.animationMode,
                        animationSpeed = settings.animationSpeed,
                        lastMove = state.currentNode.move,
                        onMoveSelected = onPlayMove,
                    )
                }
            }

            item {
                Text("Critical Moments & Move Classifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Mistakes & Blunders", "White", "Black").forEach { f ->
                        FilterChip(selected = moveFilter == f, onClick = { moveFilter = f }, label = { Text(f) })
                    }
                }
            }

            val filteredMoves = report.moves.filter { rm ->
                when (moveFilter) {
                    "Mistakes & Blunders" -> rm.mistakeBreakdown != null || rm.isCriticalMoment
                    "White" -> rm.ply % 2 == 1
                    "Black" -> rm.ply % 2 == 0
                    else -> true
                }
            }

            items(filteredMoves, key = { it.ply }) { rm ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onJumpToPly(rm.ply) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (rm.isCriticalMoment) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${rm.ply}. ${rm.san}", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MoveQualityBadge(quality = rm.quality, compact = true)
                                Text("-${rm.cpLoss} cp", style = MaterialTheme.typography.labelSmall)
                                TextButton(onClick = { inspectedMove = rm }) {
                                    Text("Why?")
                                }
                            }
                        }
                        Text(rm.coachExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        rm.mistakeBreakdown?.let { bd ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Why Was This a Mistake?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text("1. Played: ${bd.movePlayedSan}  |  2. Better Move: ${bd.bestMoveSan}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("3. Eval Shift: ${bd.evalBeforeFormatted} -> ${bd.evalAfterFormatted}", style = MaterialTheme.typography.bodySmall)
                                    Text("4. What Changed: ${bd.whatChanged}", style = MaterialTheme.typography.bodySmall)
                                    Text("5. Opponent Threat: ${bd.opponentThreatCreated}", style = MaterialTheme.typography.bodySmall)
                                    Text("6. Tactical Reason: ${bd.tacticalReason}", style = MaterialTheme.typography.bodySmall)
                                    Text("7. Material Impact: ${bd.materialConsequence}", style = MaterialTheme.typography.bodySmall)
                                    Text("8. Positional Note: ${bd.positionExplanation}", style = MaterialTheme.typography.bodySmall)
                                    Text("9. Improvement: ${bd.suggestedImprovement}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    OutlinedButton(
                                        onClick = { onPlayFromPosition(rm.fenBefore) },
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        Text("10. Retry / Practice Position")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedMove?.let { rm ->
        MoveExplanationDialog(
            reviewedMove = rm,
            onDismiss = { inspectedMove = null },
        )
    }
}

@Composable
private fun ExploreMasterAndGambitsTab(
    onLoadPgn: (String) -> Unit,
) {
    var section by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Gamebit Explore & Repertoire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("Master Games (${MasterGamesCatalog.FAMOUS_GAMES.size})") })
                FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("Named Gambits (${OpeningBook.NAMED_GAMBITS.size})") })
            }
        }

        if (section == 0) {
            items(MasterGamesCatalog.FAMOUS_GAMES, key = { it.id }) { game ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(game.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${game.white} vs ${game.black} (${game.year}) • ${game.eco} ${game.opening} • ${game.result}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(game.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { onLoadPgn(game.pgn) }) {
                            Text("Watch & Analyze")
                        }
                    }
                }
            }
        } else {
            items(OpeningBook.NAMED_GAMBITS, key = { it.name }) { gambit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${gambit.eco} • ${gambit.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(gambit.side, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(gambit.recommendedIdeas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            val pgn = buildPgnFromUciList(gambit.name, gambit.acceptedLineUci)
                            onLoadPgn(pgn)
                        }) {
                            Text("Explore Gambit Line")
                        }
                    }
                }
            }
        }
    }
}

private fun buildPgnFromUciList(title: String, uciMoves: List<String>): String {
    var st = ChessBoardState.standard()
    val root = MoveNode(state = st, move = null, san = "")
    var curr = root
    for (uci in uciMoves) {
        val legal = MoveGenerator.legalMoves(st)
        val mv = legal.firstOrNull { it.uci == uci } ?: break
        val san = MoveGenerator.toSan(st, mv, legal)
        st = MoveGenerator.applyMoveUnchecked(st, mv)
        curr = curr.addOrGetChild(mv, san, st)
    }
    return PgnParser.exportPgn(root, mapOf("Event" to title), "*")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardEditorTab(
    initialFen: String,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onLoadToAnalysis: (String) -> Unit,
    onPlayFromFen: (String) -> Unit,
    onShareText: (String, String) -> Unit,
) {
    var editorState by remember { mutableStateOf(ChessBoardState.fromFen(initialFen).getOrElse { ChessBoardState.standard() }) }
    var selectedBrush by remember { mutableStateOf<Piece?>(Piece(PieceType.QUEEN, PieceColor.WHITE)) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val palette = listOf<Piece?>(
        null,
        Piece(PieceType.KING, PieceColor.WHITE),
        Piece(PieceType.QUEEN, PieceColor.WHITE),
        Piece(PieceType.ROOK, PieceColor.WHITE),
        Piece(PieceType.BISHOP, PieceColor.WHITE),
        Piece(PieceType.KNIGHT, PieceColor.WHITE),
        Piece(PieceType.PAWN, PieceColor.WHITE),
        Piece(PieceType.KING, PieceColor.BLACK),
        Piece(PieceType.QUEEN, PieceColor.BLACK),
        Piece(PieceType.ROOK, PieceColor.BLACK),
        Piece(PieceType.BISHOP, PieceColor.BLACK),
        Piece(PieceType.KNIGHT, PieceColor.BLACK),
        Piece(PieceType.PAWN, PieceColor.BLACK),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Custom Board Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Select a piece (or Clear) and tap any square.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ChessBoardView(
                    state = editorState,
                    legalMoves = emptyList(),
                    theme = boardTheme,
                    modifier = Modifier.widthIn(max = 460.dp),
                    interactive = true,
                    pieceStyleId = settings.pieceStyleId,
                    moveAnimation = false,
                    onSquareTappedRaw = { sq ->
                        val nextBoard = editorState.board.copyOf()
                        nextBoard[sq.index] = selectedBrush
                        editorState = editorState.copy(board = nextBoard)
                        validationMessage = null
                    },
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(palette) { piece ->
                    val isSel = selectedBrush == piece
                    Surface(
                        modifier = Modifier.size(48.dp).clickable { selectedBrush = piece },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (piece == null) Text("Del", style = MaterialTheme.typography.labelSmall)
                            else PieceMiniIcon(piece = piece, modifier = Modifier.size(34.dp), pieceStyleId = settings.pieceStyleId)
                        }
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = editorState.sideToMove == PieceColor.WHITE,
                    onClick = { editorState = editorState.copy(sideToMove = PieceColor.WHITE) },
                    label = { Text("White to Move") },
                )
                FilterChip(
                    selected = editorState.sideToMove == PieceColor.BLACK,
                    onClick = { editorState = editorState.copy(sideToMove = PieceColor.BLACK) },
                    label = { Text("Black to Move") },
                )
                OutlinedButton(onClick = { editorState = ChessBoardState.standard() }) { Text("Reset") }
                OutlinedButton(onClick = {
                    editorState = ChessBoardState.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").getOrThrow()
                }) { Text("Clear (Kings)") }
                OutlinedButton(onClick = {
                    val cr = editorState.castlingRights
                    editorState = editorState.copy(
                        castlingRights = if (cr == CastlingRights.NONE) CastlingRights() else CastlingRights.NONE,
                    )
                }) {
                    Text(if (editorState.castlingRights == CastlingRights.NONE) "Enable Castling" else "Disable Castling")
                }
            }
        }
        item {
            val fen = editorState.toFen()
            Text("FEN: $fen", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            val err = editorState.validateForEditor().firstOrNull()
            if (err != null) {
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (validationMessage != null) {
                Text(validationMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(
                    onClick = {
                        if (err == null) onLoadToAnalysis(fen) else validationMessage = err
                    },
                    enabled = err == null,
                    modifier = Modifier.weight(1f),
                ) { Text("Analyze") }
                FilledTonalButton(
                    onClick = {
                        if (err == null) onPlayFromFen(fen) else validationMessage = err
                    },
                    enabled = err == null,
                    modifier = Modifier.weight(1f),
                ) { Text("Play Bot") }
                OutlinedButton(onClick = { onShareText("Gamebit FEN", fen) }) { Text("Share") }
            }
        }
    }
}
