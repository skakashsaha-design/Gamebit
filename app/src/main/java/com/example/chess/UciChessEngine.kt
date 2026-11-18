package com.example.chess

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure-Kotlin UCI (Universal Chess Interface) protocol engine wrapping ChessEngine.
 * Honestly identifies itself as ChessHub Kotlin UCI Engine (does not claim an external Stockfish binary).
 */
class UciChessEngine(
    lowMemoryMode: Boolean = false,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine = ChessEngine(lowMemoryMode = lowMemoryMode)
    private var currentState: ChessBoardState = ChessBoardState.standard()
    private var searchJob: Job? = null

    var optionThreads: Int = 1
        private set
    var optionHashMb: Int = if (lowMemoryMode) 8 else 16
        private set
    var optionMultiPv: Int = if (lowMemoryMode) 1 else 3
        private set
    var optionOwnBook: Boolean = true
        private set

    private val _outputLog = MutableStateFlow<List<String>>(emptyList())
    val outputLog: StateFlow<List<String>> = _outputLog.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isRunning = MutableStateFlow(true)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private fun emitLine(line: String) {
        _outputLog.value = (_outputLog.value + line).takeLast(120)
    }

    suspend fun sendCommand(rawCommand: String): List<String> {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return emptyList()
        if (!_isRunning.value && cmd != "uci") {
            restartEngine()
        }
        val tokens = cmd.split(Regex("\\s+"))
        val emitted = mutableListOf<String>()
        fun out(line: String) {
            emitted.add(line)
            emitLine(line)
        }

        when (tokens[0]) {
            "uci" -> {
                out("id name ChessHub Kotlin UCI Engine 2.0")
                out("id author ChessHub Project (Pure Kotlin)")
                out("option name Threads type spin default $optionThreads min 1 max 4")
                out("option name Hash type spin default $optionHashMb min 4 max 64")
                out("option name MultiPV type spin default $optionMultiPv min 1 max 3")
                out("option name OwnBook type check default $optionOwnBook")
                out("uciok")
            }
            "isready" -> {
                out("readyok")
            }
            "ucinewgame" -> {
                searchJob?.cancelAndJoin()
                currentState = ChessBoardState.standard()
                engine = ChessEngine(lowMemoryMode = optionHashMb <= 8)
                out("info string new game initialized")
            }
            "setoption" -> {
                val nameIdx = tokens.indexOf("name")
                val valIdx = tokens.indexOf("value")
                if (nameIdx >= 0 && valIdx > nameIdx) {
                    val optName = tokens.subList(nameIdx + 1, valIdx).joinToString(" ").lowercase()
                    val optVal = tokens.subList(valIdx + 1, tokens.size).joinToString(" ")
                    when (optName) {
                        "threads" -> optionThreads = optVal.toIntOrNull()?.coerceIn(1, 4) ?: optionThreads
                        "hash" -> {
                            optionHashMb = optVal.toIntOrNull()?.coerceIn(4, 64) ?: optionHashMb
                            engine = ChessEngine(lowMemoryMode = optionHashMb <= 8)
                        }
                        "multipv" -> optionMultiPv = optVal.toIntOrNull()?.coerceIn(1, 3) ?: optionMultiPv
                        "ownbook" -> optionOwnBook = optVal.equals("true", ignoreCase = true)
                    }
                    out("info string setoption $optName = $optVal")
                }
            }
            "position" -> {
                parsePositionCommand(tokens)
                out("info string position loaded (${currentState.sideToMove.name.lowercase()} to move)")
            }
            "go" -> {
                val depthIdx = tokens.indexOf("depth")
                val moveTimeIdx = tokens.indexOf("movetime")
                val infinite = "infinite" in tokens
                val targetDepth = if (depthIdx >= 0) tokens.getOrNull(depthIdx + 1)?.toIntOrNull()?.coerceIn(1, 5) ?: 3 else if (infinite) 4 else 3
                val timeLimit = if (moveTimeIdx >= 0) tokens.getOrNull(moveTimeIdx + 1)?.toLongOrNull()?.coerceIn(150L, 6000L) ?: 1500L else if (infinite) 4500L else 1800L

                searchJob?.cancelAndJoin()
                _isSearching.value = true
                var lastBestMove = "0000"
                val snapshot = currentState
                val job = scope.launch {
                    try {
                        engine.analyzeStream(
                            state = snapshot,
                            maxDepth = targetDepth,
                            multiPv = optionMultiPv,
                            timeLimitMs = timeLimit,
                        ).collect { lines ->
                            lines.forEach { line ->
                                val scoreStr = if (line.mateIn != null) "mate ${line.mateIn}" else "cp ${line.scoreCp}"
                                val pvUci = line.pvMoves.joinToString(" ") { it.uci }
                                out("info depth ${line.depth} multipv ${line.multipvRank} score $scoreStr nodes ${line.nodes} nps ${line.nps} pv $pvUci")
                            }
                            lines.firstOrNull()?.pvMoves?.firstOrNull()?.uci?.let { lastBestMove = it }
                        }
                    } finally {
                        _isSearching.value = false
                        out("bestmove $lastBestMove")
                    }
                }
                searchJob = job
                if (!infinite) {
                    job.join()
                }
            }
            "stop" -> {
                searchJob?.cancelAndJoin()
                _isSearching.value = false
            }
            "quit" -> {
                searchJob?.cancelAndJoin()
                _isSearching.value = false
                _isRunning.value = false
                out("info string engine stopped")
            }
            else -> {
                out("info string unknown command: $cmd")
            }
        }
        return emitted
    }

    fun restartEngine() {
        searchJob?.cancel()
        engine = ChessEngine(lowMemoryMode = optionHashMb <= 8)
        _isRunning.value = true
        _isSearching.value = false
        emitLine("info string engine restarted cleanly")
    }

    fun clearLog() {
        _outputLog.value = emptyList()
    }

    private fun parsePositionCommand(tokens: List<String>) {
        if (tokens.size < 2) return
        val movesIdx = tokens.indexOf("moves")
        var state = when (tokens[1]) {
            "startpos" -> ChessBoardState.standard()
            "fen" -> {
                val endIdx = if (movesIdx >= 0) movesIdx else tokens.size
                val fen = tokens.subList(2, endIdx).joinToString(" ")
                ChessBoardState.fromFen(fen).getOrDefault(ChessBoardState.standard())
            }
            else -> ChessBoardState.standard()
        }
        if (movesIdx >= 0 && movesIdx + 1 < tokens.size) {
            for (uci in tokens.subList(movesIdx + 1, tokens.size)) {
                val legal = MoveGenerator.legalMoves(state)
                val match = legal.firstOrNull { it.uci == uci.lowercase() } ?: break
                state = MoveGenerator.applyMoveUnchecked(state, match)
            }
        }
        currentState = state
    }
}
