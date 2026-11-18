package com.example.chess

data class PgnGame(
    val headers: Map<String, String>,
    val rootNode: MoveNode,
    val result: String,
    val variant: ChessVariant,
    val initialFen: String,
)

object PgnParser {
    fun parseMultiGamePgn(pgnText: String): Result<List<PgnGame>> = runCatching {
        val trimmed = pgnText.trim()
        require(trimmed.isNotEmpty()) { "PGN text is empty." }

        val games = mutableListOf<PgnGame>()
        val lines = trimmed.lines()
        val currentHeaders = linkedMapOf<String, String>()
        val moveTextBuilder = StringBuilder()
        var readingMoves = false

        fun flushCurrentGame() {
            if (currentHeaders.isNotEmpty() || moveTextBuilder.isNotBlank()) {
                games.add(buildSingleGame(currentHeaders.toMap(), moveTextBuilder.toString()))
                currentHeaders.clear()
                moveTextBuilder.clear()
                readingMoves = false
            }
        }

        val headerRegex = Regex("""^\[([A-Za-z0-9_]+)\s+"(.*)"\]\s*$""")
        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty()) continue
            val match = headerRegex.matchEntire(t)
            if (match != null) {
                if (readingMoves) {
                    flushCurrentGame()
                }
                currentHeaders[match.groupValues[1]] = match.groupValues[2]
            } else {
                readingMoves = true
                moveTextBuilder.append(t).append(' ')
            }
        }
        flushCurrentGame()
        require(games.isNotEmpty()) { "No valid PGN game found." }
        games
    }

    private fun buildSingleGame(headers: Map<String, String>, moveSection: String): PgnGame {
        val variantName = headers["Variant"] ?: "Standard"
        val variant = ChessVariant.entries.firstOrNull {
            it.displayName.equals(variantName, ignoreCase = true) ||
                it.shortCode.equals(variantName, ignoreCase = true)
        } ?: ChessVariant.STANDARD

        val initialFen = headers["FEN"] ?: ChessBoardState.initialForVariant(variant).toFen()
        val initialState = ChessBoardState.fromFen(initialFen, variant).getOrThrow()
        val root = MoveNode(id = "root", state = initialState, move = null, san = "")

        val tokens = tokenizeMoves(moveSection)
        var result = headers["Result"] ?: "*"
        val stack = ArrayDeque<MoveNode>()
        var current = root

        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx++]
            when {
                token == "(" -> {
                    val branchBase = current.parent ?: root
                    stack.addLast(current)
                    current = branchBase
                }
                token == ")" -> {
                    if (stack.isNotEmpty()) {
                        current = stack.removeLast()
                    }
                }
                token.startsWith("{") && token.endsWith("}") -> {
                    val comment = token.substring(1, token.length - 1).trim()
                    current.comment = if (current.comment.isBlank()) comment else "${current.comment} $comment"
                }
                token.startsWith("$") -> {
                    token.drop(1).toIntOrNull()?.let { nag ->
                        if (nag !in current.nags) current.nags.add(nag)
                    }
                }
                token in setOf("1-0", "0-1", "1/2-1/2", "*") -> {
                    result = token
                }
                token.endsWith(".") || token.all { it.isDigit() || it == '.' } -> {
                    // Move number token, ignore
                }
                else -> {
                    val cleanSan = extractInlineNag(token, current)
                    if (cleanSan.isNotEmpty()) {
                        val move = MoveGenerator.parseSan(current.state, cleanSan)
                            ?: error("Illegal or unrecognized move '$token' at ${current.state.toFen()}")
                        val nextState = MoveGenerator.applyMoveUnchecked(current.state, move)
                        val formattedSan = MoveGenerator.toSan(current.state, move)
                        val child = current.addOrGetChild(move, formattedSan, nextState)
                        current = child
                    }
                }
            }
        }

        return PgnGame(
            headers = headers,
            rootNode = root,
            result = result,
            variant = variant,
            initialFen = initialFen,
        )
    }

    private fun extractInlineNag(token: String, node: MoveNode): String {
        val suffixes = mapOf("!!" to 3, "!?" to 5, "?!" to 6, "??" to 4, "!" to 1, "?" to 2)
        for ((suffix, nag) in suffixes) {
            if (token.endsWith(suffix)) {
                val base = token.removeSuffix(suffix)
                // We attach NAG after move creation if needed; keep clean SAN here
                return base
            }
        }
        return token
    }

    private fun tokenizeMoves(text: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch.isWhitespace() -> i++
                ch == '{' -> {
                    val end = text.indexOf('}', i + 1)
                    if (end == -1) {
                        tokens.add("{${text.substring(i + 1)}}")
                        break
                    } else {
                        tokens.add(text.substring(i, end + 1))
                        i = end + 1
                    }
                }
                ch == ';' -> {
                    val end = text.indexOf('\n', i + 1)
                    i = if (end == -1) text.length else end + 1
                }
                ch == '(' || ch == ')' -> {
                    tokens.add(ch.toString())
                    i++
                }
                else -> {
                    val start = i
                    while (i < text.length && !text[i].isWhitespace() && text[i] !in charArrayOf('(', ')', '{', '}')) {
                        i++
                    }
                    tokens.add(text.substring(start, i))
                }
            }
        }
        return tokens
    }

    fun exportPgn(
        root: MoveNode,
        headers: Map<String, String>,
        result: String = "*",
        includeAnnotations: Boolean = true,
    ): String {
        val sb = StringBuilder()
        val orderedHeaders = linkedMapOf(
            "Event" to (headers["Event"] ?: "ChessHub Game"),
            "Site" to (headers["Site"] ?: "ChessHub"),
            "Date" to (headers["Date"] ?: "2026.11.17"),
            "Round" to (headers["Round"] ?: "-"),
            "White" to (headers["White"] ?: "White"),
            "Black" to (headers["Black"] ?: "Black"),
            "Result" to result,
        )
        headers.forEach { (k, v) -> orderedHeaders.putIfAbsent(k, v) }
        orderedHeaders.forEach { (k, v) ->
            sb.append("[$k \"$v\"]\n")
        }
        sb.append('\n')
        appendVariation(sb, root, includeAnnotations, forceMoveNumber = true)
        sb.append(' ').append(result)
        return sb.toString().trim()
    }

    private fun appendVariation(
        sb: StringBuilder,
        parent: MoveNode,
        includeAnnotations: Boolean,
        forceMoveNumber: Boolean,
    ) {
        var node = parent
        var needNum = forceMoveNumber
        while (node.children.isNotEmpty()) {
            val main = node.children.first()
            val moveNum = node.state.fullMoveNumber
            if (node.state.sideToMove == PieceColor.WHITE) {
                sb.append("$moveNum. ")
            } else if (needNum) {
                sb.append("$moveNum... ")
            }
            sb.append(main.san)
            if (includeAnnotations) {
                main.nags.forEach { nag -> sb.append(" $$nag") }
                if (main.comment.isNotBlank()) sb.append(" {${main.comment}}")
            }
            sb.append(' ')
            needNum = false

            if (includeAnnotations && node.children.size > 1) {
                for (vIdx in 1 until node.children.size) {
                    val alt = node.children[vIdx]
                    sb.append("(")
                    if (node.state.sideToMove == PieceColor.WHITE) sb.append("$moveNum. ")
                    else sb.append("$moveNum... ")
                    sb.append(alt.san)
                    alt.nags.forEach { nag -> sb.append(" $$nag") }
                    if (alt.comment.isNotBlank()) sb.append(" {${alt.comment}}")
                    sb.append(' ')
                    appendVariation(sb, alt, includeAnnotations = true, forceMoveNumber = false)
                    if (sb.endsWith(' ')) sb.deleteCharAt(sb.length - 1)
                    sb.append(") ")
                    needNum = true
                }
            }
            node = main
        }
    }
}
