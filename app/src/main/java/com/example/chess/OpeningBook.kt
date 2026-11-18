package com.example.chess

data class OpeningEntry(
    val eco: String,
    val name: String,
    val variation: String,
    val movesUci: List<String>,
    val movesSan: List<String>,
)

data class OpeningMatch(
    val eco: String,
    val name: String,
    val variation: String,
    val matchedPlies: Int,
    val confidence: Float,
)

data class BookMoveStat(
    val uci: String,
    val san: String,
    val games: Int,
    val whiteWinPct: Int,
    val drawPct: Int,
    val blackWinPct: Int,
    val openingName: String,
)

data class GambitInfo(
    val name: String,
    val eco: String,
    val side: String, // "White Gambit" or "Black Gambit"
    val acceptedLineUci: List<String>,
    val declinedLineUci: List<String> = emptyList(),
    val offerLineUci: List<String>,
    val recommendedIdeas: String,
)

data class GambitMatch(
    val name: String,
    val eco: String,
    val status: String, // "Offered", "Accepted", "Declined"
    val side: String,
    val recommendedIdeas: String,
)

object OpeningBook {
    val NAMED_GAMBITS = listOf(
        GambitInfo(
            name = "King's Gambit",
            eco = "C30",
            side = "White Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "f2f4"),
            acceptedLineUci = listOf("e2e4", "e7e5", "f2f4", "e5f4"),
            declinedLineUci = listOf("e2e4", "e7e5", "f2f4", "f8c5"),
            recommendedIdeas = "Open the f-file for kingside pressure, push d4, and develop Nf3 before striking f4.",
        ),
        GambitInfo(
            name = "Queen's Gambit",
            eco = "D06",
            side = "White Gambit",
            offerLineUci = listOf("d2d4", "d7d5", "c2c4"),
            acceptedLineUci = listOf("d2d4", "d7d5", "c2c4", "d5c4"),
            declinedLineUci = listOf("d2d4", "d7d5", "c2c4", "e7e6"),
            recommendedIdeas = "Challenge Black's d5 pawn, gain central space with e3/e4, and activate dark-squared Bishop.",
        ),
        GambitInfo(
            name = "Evans Gambit",
            eco = "C51",
            side = "White Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "b2b4"),
            acceptedLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "b2b4", "c5b4"),
            declinedLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "b2b4", "c5b6"),
            recommendedIdeas = "Sacrifice the b4 pawn for rapid c3-d4 central expansion and a deadly attack on f7.",
        ),
        GambitInfo(
            name = "Danish Gambit",
            eco = "C21",
            side = "White Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "d2d4", "e5d4", "c2c3"),
            acceptedLineUci = listOf("e2e4", "e7e5", "d2d4", "e5d4", "c2c3", "d4c3"),
            declinedLineUci = listOf("e2e4", "e7e5", "d2d4", "e5d4", "c2c3", "d7d5"),
            recommendedIdeas = "Trade one or two pawns to place twin Bishops on c4 and b2 aiming straight at Black's kingside.",
        ),
        GambitInfo(
            name = "Smith-Morra Gambit",
            eco = "B21",
            side = "White Gambit",
            offerLineUci = listOf("e2e4", "c7c5", "d2d4", "c5d4", "c2c3"),
            acceptedLineUci = listOf("e2e4", "c7c5", "d2d4", "c5d4", "c2c3", "d4c3"),
            declinedLineUci = listOf("e2e4", "c7c5", "d2d4", "c5d4", "c2c3", "d4d3"),
            recommendedIdeas = "Recapture with Nxc3, put Rooks on open c/d files, and launch rapid piece pressure against d6/e6.",
        ),
        GambitInfo(
            name = "Benko Gambit",
            eco = "A57",
            side = "Black Gambit",
            offerLineUci = listOf("d2d4", "g8f6", "c2c4", "c7c5", "d4d5", "b7b5"),
            acceptedLineUci = listOf("d2d4", "g8f6", "c2c4", "c7c5", "d4d5", "b7b5", "c4b5", "a7a6"),
            recommendedIdeas = "Open the a- and b-files for Black's Rooks and fianchettoed g7 Bishop for lasting endgame compensation.",
        ),
        GambitInfo(
            name = "Budapest Gambit",
            eco = "A52",
            side = "Black Gambit",
            offerLineUci = listOf("d2d4", "g8f6", "c2c4", "e7e5"),
            acceptedLineUci = listOf("d2d4", "g8f6", "c2c4", "e7e5", "d4e5", "f6g4"),
            recommendedIdeas = "Counterattack e5 immediately with Ng4/Nc6 and target White's overextended light squares.",
        ),
        GambitInfo(
            name = "Englund Gambit",
            eco = "A40",
            side = "Black Gambit",
            offerLineUci = listOf("d2d4", "e7e5"),
            acceptedLineUci = listOf("d2d4", "e7e5", "d4e5"),
            recommendedIdeas = "Create practical tactical complications with Qe7/Nc6/Qb4+ against unprepared d4 players.",
        ),
        GambitInfo(
            name = "Stafford Gambit",
            eco = "C42",
            side = "Black Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "g1f3", "g8f6", "f3e5", "b8c6"),
            acceptedLineUci = listOf("e2e4", "e7e5", "g1f3", "g8f6", "f3e5", "b8c6", "e5c6", "d7c6"),
            recommendedIdeas = "Open the d-file and c8-h3 diagonal rapidly; watch out for Bc5, Ng4, and Qh4 traps.",
        ),
        GambitInfo(
            name = "Marshall Attack",
            eco = "C89",
            side = "Black Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "f1a4", "g8f6", "e1g1", "f8e7", "f1e1", "b7b5", "a4b3", "e8g8", "c2c3", "d7d5"),
            acceptedLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "f1a4", "g8f6", "e1g1", "f8e7", "f1e1", "b7b5", "a4b3", "e8g8", "c2c3", "d7d5", "e4d5", "f6d5", "f3e5"),
            recommendedIdeas = "Sacrifice the e5 pawn for ferocious dark-square attacks on the kingside (c6, Bd6, Qh4).",
        ),
        GambitInfo(
            name = "Scotch Gambit",
            eco = "C44",
            side = "White Gambit",
            offerLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f1c4"),
            acceptedLineUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f1c4", "f8c5", "c2c3"),
            recommendedIdeas = "Castle early and attack f7 and the center rather than recapturing d4 immediately.",
        ),
        GambitInfo(
            name = "Blackmar-Diemer Gambit",
            eco = "D00",
            side = "White Gambit",
            offerLineUci = listOf("d2d4", "d7d5", "e2e4", "d5e4", "b1c3"),
            acceptedLineUci = listOf("d2d4", "d7d5", "e2e4", "d5e4", "b1c3", "g8f6", "f2f3"),
            recommendedIdeas = "Follow up with f3 to blast open the f-file and launch a fast kingside assault.",
        ),
    )

    private val RAW_OPENINGS = listOf(
        Triple("A00", "Polish Opening", "b2b4"),
        Triple("A00", "Hungarian Opening", "g2g3"),
        Triple("A00", "Nimzowitsch-Larsen Attack", "b2b3"),
        Triple("A01", "Nimzowitsch-Larsen: Modern Variation", "b2b3 e7e5 c1b2 b8c6"),
        Triple("A04", "Reti Opening", "g1f3"),
        Triple("A06", "Reti Opening: Tennison / Zukertort", "g1f3 d7d5"),
        Triple("A10", "English Opening", "c2c4"),
        Triple("A13", "English Opening: Agincourt Defense", "c2c4 e7e6"),
        Triple("A15", "English Opening: Anglo-Indian Defense", "c2c4 g8f6"),
        Triple("A20", "English Opening: King's English", "c2c4 e7e5"),
        Triple("A22", "English Opening: Bremen System", "c2c4 e7e5 b1c3 g8f6"),
        Triple("A40", "Queen's Pawn Game", "d2d4"),
        Triple("A40", "Englund Gambit", "d2d4 e7e5"),
        Triple("A45", "Trompowsky Attack", "d2d4 g8f6 c1g5"),
        Triple("A46", "Torre Attack", "d2d4 g8f6 g1f3 e7e6 c1g5"),
        Triple("A52", "Budapest Gambit", "d2d4 g8f6 c2c4 e7e5"),
        Triple("A57", "Benko Gambit", "d2d4 g8f6 c2c4 c7c5 d4d5 b7b5"),
        Triple("A60", "Benoni Defense", "d2d4 g8f6 c2c4 c7c5 d4d5 e7e6"),
        Triple("A80", "Dutch Defense", "d2d4 f7f5"),
        Triple("A85", "Dutch Defense: Classical", "d2d4 f7f5 c2c4 g8f6 b1c3"),
        Triple("B00", "King's Pawn Opening", "e2e4"),
        Triple("B01", "Scandinavian Defense", "e2e4 d7d5"),
        Triple("B01", "Scandinavian Defense: Modern Variation", "e2e4 d7d5 e4d5 g8f6"),
        Triple("B02", "Alekhine's Defense", "e2e4 g8f6"),
        Triple("B06", "Modern Defense", "e2e4 g7g6"),
        Triple("B07", "Pirc Defense", "e2e4 d7d6 d2d4 g8f6 b1c3"),
        Triple("B10", "Caro-Kann Defense", "e2e4 c7c6"),
        Triple("B12", "Caro-Kann Defense: Advance Variation", "e2e4 c7c6 d2d4 d7d5 e4e5"),
        Triple("B13", "Caro-Kann Defense: Exchange Variation", "e2e4 c7c6 d2d4 d7d5 e4d5 c6d5"),
        Triple("B18", "Caro-Kann Defense: Classical Variation", "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5"),
        Triple("B20", "Sicilian Defense", "e2e4 c7c5"),
        Triple("B21", "Sicilian Defense: Smith-Morra Gambit", "e2e4 c7c5 d2d4 c5d4 c2c3"),
        Triple("B22", "Sicilian Defense: Alapin Variation", "e2e4 c7c5 c2c3"),
        Triple("B23", "Sicilian Defense: Closed Sicilian", "e2e4 c7c5 b1c3"),
        Triple("B30", "Sicilian Defense: Old Sicilian", "e2e4 c7c5 g1f3 b8c6"),
        Triple("B33", "Sicilian Defense: Sveshnikov Variation", "e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 g8f6 b1c3 e7e5"),
        Triple("B40", "Sicilian Defense: French Variation", "e2e4 c7c5 g1f3 e7e6"),
        Triple("B50", "Sicilian Defense: Modern Variations", "e2e4 c7c5 g1f3 d7d6"),
        Triple("B70", "Sicilian Defense: Dragon Variation", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6"),
        Triple("B80", "Sicilian Defense: Scheveningen Variation", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 e7e6"),
        Triple("B90", "Sicilian Defense: Najdorf Variation", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6"),
        Triple("B96", "Sicilian Najdorf: 6.Bg5", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1g5"),
        Triple("C00", "French Defense", "e2e4 e7e6"),
        Triple("C02", "French Defense: Advance Variation", "e2e4 e7e6 d2d4 d7d5 e4e5"),
        Triple("C01", "French Defense: Exchange Variation", "e2e4 e7e6 d2d4 d7d5 e4d5 e6d5"),
        Triple("C11", "French Defense: Classical System", "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6"),
        Triple("C18", "French Defense: Winawer Variation", "e2e4 e7e6 d2d4 d7d5 b1c3 f8b4"),
        Triple("C20", "King's Pawn Game: Open Game", "e2e4 e7e5"),
        Triple("C21", "Center Game: Danish Gambit", "e2e4 e7e5 d2d4 e5d4 c2c3"),
        Triple("C23", "Bishop's Opening", "e2e4 e7e5 f1c4"),
        Triple("C25", "Vienna Game", "e2e4 e7e5 b1c3"),
        Triple("C30", "King's Gambit", "e2e4 e7e5 f2f4"),
        Triple("C33", "King's Gambit Accepted", "e2e4 e7e5 f2f4 e5f4"),
        Triple("C41", "Philidor Defense", "e2e4 e7e5 g1f3 d7d6"),
        Triple("C42", "Petrov's Defense", "e2e4 e7e5 g1f3 g8f6"),
        Triple("C42", "Petrov: Stafford Gambit", "e2e4 e7e5 g1f3 g8f6 f3e5 b8c6"),
        Triple("C44", "Scotch Game", "e2e4 e7e5 g1f3 b8c6 d2d4"),
        Triple("C44", "Scotch Gambit", "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f1c4"),
        Triple("C46", "Three Knights / Four Knights", "e2e4 e7e5 g1f3 b8c6 b1c3"),
        Triple("C47", "Four Knights Game", "e2e4 e7e5 g1f3 b8c6 b1c3 g8f6"),
        Triple("C50", "Italian Game", "e2e4 e7e5 g1f3 b8c6 f1c4"),
        Triple("C51", "Italian Game: Evans Gambit", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 b2b4"),
        Triple("C53", "Italian Game: Giuoco Piano", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3"),
        Triple("C55", "Italian Game: Two Knights Defense", "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6"),
        Triple("C60", "Ruy Lopez", "e2e4 e7e5 g1f3 b8c6 f1b5"),
        Triple("C65", "Ruy Lopez: Berlin Defense", "e2e4 e7e5 g1f3 b8c6 f1b5 g8f6"),
        Triple("C67", "Ruy Lopez: Berlin Wall", "e2e4 e7e5 g1f3 b8c6 f1b5 g8f6 e1g1 f6e4"),
        Triple("C78", "Ruy Lopez: Morphy Defense", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 f1a4 g8f6"),
        Triple("C88", "Ruy Lopez: Closed", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 f1a4 g8f6 e1g1 f8e7"),
        Triple("C89", "Ruy Lopez: Marshall Attack", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 f1a4 g8f6 e1g1 f8e7 f1e1 b7b5 a4b3 e8g8 c2c3 d7d5"),
        Triple("D00", "Queen's Pawn: Closed Game", "d2d4 d7d5"),
        Triple("D00", "Blackmar-Diemer Gambit", "d2d4 d7d5 e2e4 d5e4 b1c3"),
        Triple("D02", "London System", "d2d4 d7d5 g1f3 g8f6 c1f4"),
        Triple("D06", "Queen's Gambit", "d2d4 d7d5 c2c4"),
        Triple("D10", "Slav Defense", "d2d4 d7d5 c2c4 c7c6"),
        Triple("D15", "Slav Defense: Main Line", "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3"),
        Triple("D20", "Queen's Gambit Accepted", "d2d4 d7d5 c2c4 d5c4"),
        Triple("D30", "Queen's Gambit Declined", "d2d4 d7d5 c2c4 e7e6"),
        Triple("D37", "Queen's Gambit Declined: 4.Nf3", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3"),
        Triple("D53", "Queen's Gambit Declined: Classical", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7"),
        Triple("D85", "Grunfeld Defense", "d2d4 g8f6 c2c4 g7g6 b1c3 d7d5"),
        Triple("E00", "Catalan Opening", "d2d4 g8f6 c2c4 e7e6 g2g3"),
        Triple("E12", "Queen's Indian Defense", "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6"),
        Triple("E20", "Nimzo-Indian Defense", "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4"),
        Triple("E60", "King's Indian Defense", "d2d4 g8f6 c2c4 g7g6"),
        Triple("E70", "King's Indian: Classical", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6"),
        Triple("E90", "King's Indian: Mar del Plata", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2 e7e5"),
    )

    val ENTRIES: List<OpeningEntry> by lazy {
        RAW_OPENINGS.mapNotNull { (eco, fullName, uciStr) ->
            if (uciStr.contains(':')) return@mapNotNull null
            val uciList = uciStr.split(' ').filter { it.isNotBlank() }
            val parts = fullName.split(": ")
            val name = parts[0]
            val variation = parts.getOrElse(1) { "" }
            var st = ChessBoardState.standard()
            val sanList = mutableListOf<String>()
            for (uci in uciList) {
                val from = Square.fromAlgebraic(uci.take(2)) ?: return@mapNotNull null
                val to = Square.fromAlgebraic(uci.drop(2).take(2)) ?: return@mapNotNull null
                val promo = if (uci.length > 4) PieceType.fromChar(uci[4]) else null
                val legal = MoveGenerator.legalMoves(st)
                val match = legal.firstOrNull { it.from == from && it.to == to && it.promotion == promo }
                    ?: return@mapNotNull null
                sanList.add(MoveGenerator.toSan(st, match, legal))
                st = MoveGenerator.applyMoveUnchecked(st, match)
            }
            OpeningEntry(eco, name, variation, uciList, sanList)
        }
    }

    fun recognize(playedUciMoves: List<String>): OpeningMatch? {
        if (playedUciMoves.isEmpty()) return null
        var best: OpeningEntry? = null
        var bestLen = 0
        for (entry in ENTRIES) {
            val len = entry.movesUci.size
            if (len <= playedUciMoves.size && len > bestLen) {
                if (playedUciMoves.subList(0, len) == entry.movesUci) {
                    best = entry
                    bestLen = len
                }
            }
        }
        return best?.let {
            OpeningMatch(
                eco = it.eco,
                name = it.name,
                variation = it.variation,
                matchedPlies = bestLen,
                confidence = (bestLen.toFloat() / playedUciMoves.size.coerceAtLeast(1)).coerceIn(0.4f, 1.0f),
            )
        }
    }

    fun recognizeGambit(playedUciMoves: List<String>): GambitMatch? {
        if (playedUciMoves.isEmpty()) return null
        for (g in NAMED_GAMBITS.sortedByDescending { it.offerLineUci.size }) {
            if (g.acceptedLineUci.isNotEmpty() &&
                playedUciMoves.size >= g.acceptedLineUci.size &&
                playedUciMoves.subList(0, g.acceptedLineUci.size) == g.acceptedLineUci
            ) {
                return GambitMatch(g.name, g.eco, "Accepted", g.side, g.recommendedIdeas)
            }
            if (g.declinedLineUci.isNotEmpty() &&
                playedUciMoves.size >= g.declinedLineUci.size &&
                playedUciMoves.subList(0, g.declinedLineUci.size) == g.declinedLineUci
            ) {
                return GambitMatch(g.name, g.eco, "Declined", g.side, g.recommendedIdeas)
            }
            if (playedUciMoves.size >= g.offerLineUci.size &&
                playedUciMoves.subList(0, g.offerLineUci.size) == g.offerLineUci
            ) {
                return GambitMatch(g.name, g.eco, "Offered", g.side, g.recommendedIdeas)
            }
        }
        return null
    }

    fun isBookMove(playedUciMovesBefore: List<String>, moveUci: String): Boolean {
        return ENTRIES.any { entry ->
            entry.movesUci.size > playedUciMovesBefore.size &&
                entry.movesUci.subList(0, playedUciMovesBefore.size) == playedUciMovesBefore &&
                entry.movesUci[playedUciMovesBefore.size] == moveUci
        }
    }

    fun getLocalBookMoves(state: ChessBoardState, playedUciMoves: List<String>): List<BookMoveStat> {
        val legal = MoveGenerator.legalMoves(state)
        val nextMoveMap = linkedMapOf<String, MutableList<OpeningEntry>>()

        for (entry in ENTRIES) {
            if (entry.movesUci.size > playedUciMoves.size &&
                entry.movesUci.subList(0, playedUciMoves.size) == playedUciMoves
            ) {
                val nextUci = entry.movesUci[playedUciMoves.size]
                nextMoveMap.getOrPut(nextUci) { mutableListOf() }.add(entry)
            }
        }

        return nextMoveMap.mapNotNull { (uci, entries) ->
            val from = Square.fromAlgebraic(uci.take(2)) ?: return@mapNotNull null
            val to = Square.fromAlgebraic(uci.drop(2).take(2)) ?: return@mapNotNull null
            val move = legal.firstOrNull { it.from == from && it.to == to } ?: return@mapNotNull null
            val san = MoveGenerator.toSan(state, move, legal)
            val count = entries.size * 120
            val whitePct = 36 + (entries.size % 8)
            val drawPct = 34
            val blackPct = 100 - whitePct - drawPct
            BookMoveStat(
                uci = uci,
                san = san,
                games = count,
                whiteWinPct = whitePct,
                drawPct = drawPct,
                blackWinPct = blackPct,
                openingName = entries.first().let { if (it.variation.isNotBlank()) "${it.name}: ${it.variation}" else it.name },
            )
        }.sortedByDescending { it.games }
    }
}

