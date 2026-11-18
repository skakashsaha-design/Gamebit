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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import com.example.chess.CastlingRights
import com.example.chess.ChessBoardState
import com.example.chess.ChessVariant
import com.example.chess.Move
import com.example.chess.MoveNode
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.local.AppSettings
import com.example.data.local.CollectionEntity
import com.example.data.local.LocalGameEntity
import com.example.data.local.LocalStudyEntity
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.EvaluationGraph
import com.example.ui.components.PieceMiniIcon
import com.example.ui.theme.ChessBoardThemes
import com.example.viewmodel.AnalysisUiState

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
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Board Editor") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Archive (${savedGames.size})") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Studies (${studies.size})") })
        }

        when (selectedTab) {
            0 -> AnalysisBoardTab(
                state = state,
                settings = settings,
                boardTheme = boardTheme,
                onPlayMove = onPlayMove,
                onToggleEngine = onToggleEngine,
                onToggleFlip = onToggleFlip,
                onJumpStart = onJumpStart,
                onStepBack = onStepBack,
                onStepForward = onStepForward,
                onJumpEnd = onJumpEnd,
                onSelectNode = onSelectNode,
                onToggleAutoplay = onToggleAutoplay,
                onSetComment = onSetComment,
                onToggleNag = onToggleNag,
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
            )
            2 -> BoardEditorTab(
                initialFen = state.currentNode.state.toFen(),
                boardTheme = boardTheme,
                onLoadToAnalysis = { fen ->
                    if (onLoadFen(fen)) selectedTab = 0
                },
                onPlayFromFen = onPlayFromPosition,
                onShareText = onShareText,
            )
            3 -> ArchiveTab(
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
    onToggleFlip: () -> Unit,
    onJumpStart: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpEnd: () -> Unit,
    onSelectNode: (MoveNode) -> Unit,
    onToggleAutoplay: () -> Unit,
    onSetComment: (String) -> Unit,
    onToggleNag: (Int) -> Unit,
    onFetchExplorer: (Boolean) -> Unit,
    onQueryTablebase: () -> Unit,
    onSaveStudy: (String, String) -> Unit,
    onShareText: (String, String) -> Unit,
    onExportPgn: () -> String,
) {
    var commentDraft by remember(state.currentNode.id) { mutableStateOf(state.currentNode.comment) }
    var studyTitle by remember { mutableStateOf("") }

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
                Text(
                    text = state.openingMatch?.let { "${it.eco} • ${it.name}" } ?: "Analysis Board",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    modifier = Modifier.weight(1f).widthIn(max = 480.dp),
                    flipped = state.flipped,
                    interactive = true,
                    allowBothSides = true,
                    showCoordinates = settings.showCoordinates,
                    showLegalMoves = settings.showLegalMoves,
                    showLastMove = settings.showLastMove,
                    autoQueen = settings.autoQueen,
                    lastMove = state.currentNode.move,
                    arrows = state.arrows,
                    onMoveSelected = onPlayMove,
                )
            }
        }

        // Navigation controls
        item {
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
                OutlinedButton(onClick = { onShareText("ChessHub PGN", onExportPgn()) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share PGN")
                }
                OutlinedButton(onClick = { onShareText("ChessHub FEN", state.currentNode.state.toFen()) }, modifier = Modifier.weight(1f)) {
                    Text("Share FEN")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GameReviewTab(
    state: AnalysisUiState,
    settings: AppSettings,
    boardTheme: com.example.ui.theme.BoardThemeColors,
    onRunReview: () -> Unit,
    onJumpToPly: (Int) -> Unit,
    onPlayMove: (Move) -> Unit,
) {
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
                    "Import a PGN, load a game from Archive, or play moves on the Analysis board, then run Game Review for accuracy percentages, phase breakdown, move classifications, and mistake drills.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Accuracy Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("White: ${report.whiteAccuracy}%", fontWeight = FontWeight.SemiBold)
                            Text("Black: ${report.blackAccuracy}%", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Phases — Opening ${report.openingAccuracy}% • Middlegame ${report.middlegameAccuracy}% • Endgame ${report.endgameAccuracy}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "White Mistakes/Blunders: ${report.whiteMistakes}/${report.whiteBlunders}  |  Black: ${report.blackMistakes}/${report.blackBlunders}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                EvaluationGraph(
                    reviewedMoves = report.moves,
                    selectedPly = state.currentNode.ply,
                    onSelectPly = onJumpToPly,
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChessBoardView(
                        state = state.currentNode.state,
                        legalMoves = state.legalMoves,
                        theme = boardTheme,
                        modifier = Modifier.widthIn(max = 420.dp),
                        showCoordinates = settings.showCoordinates,
                        lastMove = state.currentNode.move,
                        onMoveSelected = onPlayMove,
                    )
                }
            }

            item {
                Text("Critical Moments & Move Classifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(report.moves, key = { it.ply }) { rm ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onJumpToPly(rm.ply) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (rm.isCriticalMoment) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${rm.ply}. ${rm.san} ${rm.quality.symbol}", fontWeight = FontWeight.SemiBold)
                            Text("${rm.quality.label} (-${rm.cpLoss} cp)", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(rm.coachExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardEditorTab(
    initialFen: String,
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
                            else PieceMiniIcon(piece = piece, modifier = Modifier.size(34.dp))
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
                OutlinedButton(onClick = { onShareText("ChessHub FEN", fen) }) { Text("Share") }
            }
        }
    }
}
